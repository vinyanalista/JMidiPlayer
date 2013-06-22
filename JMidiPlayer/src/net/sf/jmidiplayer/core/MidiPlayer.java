package net.sf.jmidiplayer.core;

import java.io.InputStream;
import java.util.*;

import javax.sound.midi.*;

import net.sf.jmidiplayer.utils.Log;


/**
 * A software component capable of playing back MIDI sequences. It acts as a
 * wrapper for the Java Sound API classes that work with MIDI, simplifying the
 * task of playing a MIDI sequence.
 * 
 * <p>
 * Its features include:
 * 
 * <li><strong>Full control of playback:</strong> the player supports starting,
 * pausing and stopping playback, moving to an arbitrary position in the
 * sequence, determining how long it is and how much of it has been played or is
 * left.</li>
 * <li><strong>Full control of volume:</strong> it is possible to set the volume
 * level for the sequence and mute or unmute the player.</li>
 * <li><strong>Looping based on bars:</strong> the player allows enabling and
 * disabling looping and setting a start and an end point, in terms of bars, for
 * the loop.</li>
 * <li><strong>Control of the playback tempo (speed):</strong> the player allows
 * changing the tempo of playback by a factor and retrieving the actual tempo in
 * beats per minute. All of the player's functionalities automatically handle
 * the effects of tempo changes, freeing the user from doing many calculations
 * related to tempo.</li>
 * <li><strong>Pitch in semitones:</strong> the player allows setting the pitch
 * offset for all notes it plays. The supported pitch range is two semitones up
 * and down from center.</li>
 * <li><strong>Individual controls for each instrument of the sequence:</strong>
 * the player allows muting or soloing individual tracks in the sequence, as
 * well as changing their volumes and retrieving their instruments. The player
 * hides from the user tracks that are not actually used to play sounds.</li>
 * <li><strong>Support for listeners:</strong> the player can notify components
 * for most of its state changes.</li>
 * </p>
 * 
 * <p>
 * Actually, the player does not offer complete support for MIDI sequences with
 * SMPTE-based timing. In such a case, it is not possible to determine the
 * number of bars for the sequence nor set the player to loop from a bar to
 * another.
 * </p>
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 * @see MidiPlayerListener
 * @see Track
 * 
 * @see MidiChannel
 * @see Sequence
 * @see Sequencer
 * @see Synthesizer
 * @see javax.sound.midi.Track
 * 
 */
public class MidiPlayer {
	public static enum Status {
		PLAYING, PAUSED, STOPPED
	}

	/* Constants */

	protected static final int ONE_MILLION = 1000000;

	/* Attributes */

	private List<MidiPlayerListener> listeners;
	private Sequence sequence;
	protected Sequencer sequencer;
	private Status status;
	private Synthesizer synthesizer;
	private List<Track> tracks;
	private VolumeController volumeController;

	private int originalDuration, originalTotalMinutes, originalTotalSeconds;
	private int actualDuration, actualTotalMinutes, actualTotalSeconds;
	private int ticksPerBar, totalBars;
	private int volume, tempoInBPM, pitch;
	private int loopStartPoint, loopEndPoint;
	private float tempoFactor;
	private boolean loop, mute;

	/**
	 * Constructs a <code>MidiPlayer</code> for playing back the specified MIDI
	 * sequence.
	 * 
	 * @param midiFile
	 *            the MIDI sequence to be played
	 */
	public MidiPlayer(InputStream midiSequence) {
		try {
			Log.write("Initializing player...");

			sequencer = MidiSystem.getSequencer();
			sequencer.open();

			synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();

			sequence = MidiSystem.getSequence(midiSequence);
			sequencer.setSequence(sequence);

			sequencer.addMetaEventListener(new MetaEventListener() {
				@Override
				public void meta(MetaMessage meta) {
					if (meta.getType() == 47) // Sequencer is done playing
						stop();
				}
			});

			volumeController = new VolumeController(synthesizer.getReceiver());
			for (Transmitter transmitter : sequencer.getTransmitters())
				transmitter.close();
			sequencer.getTransmitter().setReceiver(volumeController);

			status = Status.STOPPED;

			originalDuration = (int) Math.floor(sequencer
					.getMicrosecondLength() / ONE_MILLION);
			originalTotalMinutes = originalDuration / 60;
			originalTotalSeconds = originalDuration % 60;

			actualDuration = originalDuration;
			actualTotalMinutes = originalTotalMinutes;
			actualTotalSeconds = originalTotalSeconds;

			listeners = new ArrayList<MidiPlayerListener>();

			volume = 100;
			tempoFactor = 1.0f;
			tempoInBPM = calculateTempoInBPM(tempoFactor);
			pitch = synthesizer.getChannels()[0].getPitchBend();

			tracks = new ArrayList<Track>(sequence.getTracks().length);

			int ticksPerBeat = 0;

			Log.write("Duration (in ticks): " + sequence.getTickLength());
			if (sequence.getDivisionType() == Sequence.PPQ) {
				ticksPerBeat = sequence.getResolution();
				Log.write("Resolution: " + sequence.getResolution()
						+ " ticks per beat");
			} else {
				Log.write("Resolution: " + sequence.getResolution()
						+ " ticks per frame");
				Log.write("WARNING: looping for MIDI sequences with SMPTE-based timing is not supported!");
			}

			int beatsPerBar = 0;
			ticksPerBar = 0;
			totalBars = 0;

			for (int t = 0; t < sequence.getTracks().length; t++) {
				Track track = new Track(t, sequence.getTracks()[t], this);
				for (int nEvent = 0; nEvent < track.midiTrack.size(); nEvent++) {
					MidiMessage message = track.midiTrack.get(nEvent)
							.getMessage();
					if (message instanceof MetaMessage) {
						MetaMessage metaMessage = (MetaMessage) message;
						// byte[] abMessage = metaMessage.getMessage();
						byte[] abData = metaMessage.getData();
						// int nDataLength = metaMessage.getLength();
						switch (metaMessage.getType()) {
						case 0x58:
							// Time signature can be thought as a fraction: 4/4,
							// 3/4 and so on
							int numerator = (abData[0] & 0xFF);
							int denominator = (1 << (abData[1] & 0xFF));
							Log.write("Time signature: " + numerator + "/"
									+ denominator);
							beatsPerBar = numerator;
							ticksPerBar = ticksPerBeat * beatsPerBar;
							Log.write("Ticks per bar: " + ticksPerBar);
							totalBars = (int) Math.ceil(sequencer
									.getTickLength() / (float) ticksPerBar);
							Log.write("Total bars: " + totalBars);
							break;
						}
					} else if (message instanceof ShortMessage) {
						ShortMessage shortMessage = (ShortMessage) message;
						switch (shortMessage.getCommand()) {
						case 0x90:
							// Channel
							track.channel = shortMessage.getChannel();
							break;
						case 0xc0:
							// Instrument
							int program = ((ShortMessage) message).getData1();
							for (Instrument instrument : synthesizer
									.getAvailableInstruments())
								if (instrument.getPatch().getProgram() == program) {
									track.setDescription(instrument.getName());
									break;
								}
						}
					}
				}
				if (track.channel != Track.CHANNEL_UNDEFINED) {
					tracks.add(track);
					if (track.channel == 9)
						track.setDescription("Percussion");
					Log.write("Track " + track.getNumber() + ": "
							+ track.getDescription());
				} else {
					Log.write("Track "
							+ track.getNumber()
							+ " serves only for control purposes and will be ignored");
				}
			}
			if (totalBars != 0) {
				loopStartPoint = 1;
				loopEndPoint = totalBars;
			} else {
				loopStartPoint = 0;
				loopEndPoint = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.write("The player is ready to start!");
	}

	/**
	 * Adds a <code>MidiPlayerListener</code> to the player.
	 * 
	 * @param listener
	 *            the <code>MidiPlayerListener</code> to be added
	 * 
	 * @see #removeMidiPlayerListener(MidiPlayerListener)
	 */

	public void addMidiPlayerListener(MidiPlayerListener listener) {
		listeners.add(listener);
	}

	/**
	 * Calculates the new tempo in beats per minute based on the sequence's
	 * original tempo and the specified tempo factor.
	 * 
	 * @param tempoFactor
	 * @return the new tempo in beats per minute
	 */
	protected int calculateTempoInBPM(float tempoFactor) {
		return (int) (sequencer.getTempoInBPM() * tempoFactor);
	}

	/**
	 * Checks if the playback position has gone out of the range from the loop
	 * start point to the loop end point. A call to this method has effect only
	 * if looping is enabled.
	 */
	protected void checkIfOutOfTheLoopRange() {
		if (loop) {
			int positionInBars = (int) (sequencer.getTickPosition() / ticksPerBar);
			if ((positionInBars < loopStartPoint)
					|| (positionInBars > loopEndPoint))
				sequencer.setTickPosition((loopStartPoint - 1) * ticksPerBar);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		synthesizer.close();
		volumeController.close();
		sequencer.close();
	}

	/**
	 * Returns the duration of the current sequence expressed in seconds. The
	 * duration of the sequence is affected by the tempo adjustment. When tempo
	 * is set to 100 (meaning 100%), this method returns the original duration
	 * of the sequence.
	 * 
	 * @return length of the sequence in seconds
	 * 
	 * @see #getPosition()
	 * @see #getTimeLeft()
	 * @see #getTotalMinutes()
	 * @see #getTotalSeconds()
	 * 
	 * @see Sequencer#getTickLength()
	 */
	public int getDuration() {
		return actualDuration;
	}

	/**
	 * Returns the end position of the loop, in bars.
	 * 
	 * @return the end position of the loop, in bars (starting with 1)
	 * 
	 * @see #isLoopEnabled()
	 * @see #getLoopStartPoint()
	 * @see #setLoopEndPoint(int)
	 */
	public int getLoopEndPoint() {
		return loopEndPoint;
	}

	/**
	 * Returns the start position of the loop, in bars.
	 * 
	 * @return the start position of the loop, in bars (starting with 1)
	 * 
	 * @see #isLoopEnabled()
	 * @see #getLoopEndPoint()
	 * @see #setLoopStartPoint(int)
	 */
	public int getLoopStartPoint() {
		return loopStartPoint;
	}

	/**
	 * Returns the minutes left to the end of the playback.
	 * 
	 * @return the minutes left to the end
	 * 
	 * @see #getTimeLeft()
	 * @see #getSecondsLeft()
	 */
	public int getMinutesLeft() {
		return getTimeLeft() / 60;
	}

	/**
	 * Returns the number of bars that composes the sequence being played.
	 * 
	 * <p>
	 * If the sequence's timecode is expressed in SMPTE, this method returns
	 * zero, as the player does not support calculating the number of bars for
	 * MIDI sequences with SMPTE-based timing.
	 * </p>
	 * 
	 * @return the number of bars that composes the sequence
	 */
	public int getNumberOfBars() {
		return totalBars;
	}

	/**
	 * Returns the upward or downward pitch offset for the player, an integer in
	 * the range of -2 to 2.
	 * 
	 * @return the bend amount in semitones (0 = no bend) for the player
	 * 
	 * @see #setPitch(int)
	 */
	public int getPitch() {
		return pitch;
	}

	/**
	 * Returns the minutes played so far.
	 * 
	 * @return the minutes played so far
	 * 
	 * @see #getPlayedSeconds()
	 * @see #getPosition()
	 */
	public int getPlayedMinutes() {
		return getPosition() / 60;
	}

	/**
	 * Returns the seconds (besides the minutes) played so far.
	 * 
	 * @return the seconds played so far
	 * 
	 * @see #getPlayedMinutes()
	 * @see #getPosition()
	 */
	public int getPlayedSeconds() {
		return getPosition() % 60;
	}

	/**
	 * Returns the current position in the sequence, expressed in seconds.
	 * 
	 * @return the current position in microseconds
	 * 
	 * @see #getDuration()
	 * @see #getTimeLeft()
	 * @see #getPlayedMinutes()
	 * @see #getPlayedSeconds()
	 * @see #goTo(int)
	 * @see #goTo(int, int)
	 * 
	 * @see Sequencer#getMicrosecondPosition()
	 */
	public int getPosition() {
		return (int) Math.floor(sequencer.getMicrosecondPosition()
				/ (ONE_MILLION * tempoFactor));
	}

	/**
	 * Returns the seconds (besides minutes) left to the end of the playback.
	 * 
	 * @return the seconds left to the end
	 * 
	 * @see #getMinutesLeft()
	 * @see #getTimeLeft()
	 */
	public int getSecondsLeft() {
		return getTimeLeft() % 60;
	}

	/**
	 * Returns the current tempo factor for the player. The default is 100
	 * (meaning 100%).
	 * 
	 * @return the current tempo factor for the player
	 * 
	 * @see #getTempoInBPM()
	 * @see #setTempoFactor(int)
	 */
	public int getTempoFactor() {
		return (int) (tempoFactor * 100);
	}

	/**
	 * Returns the current tempo, expressed in beats per minute. The actual
	 * tempo of playback is the product of the returned value and the tempo
	 * factor.
	 * 
	 * @return the current tempo in beats per minute
	 * 
	 * @see #getTempoFactor()
	 * @see #setTempoFactor(int)
	 * 
	 * @see Sequencer#getTempoInBPM()
	 */
	public int getTempoInBPM() {
		return tempoInBPM;
	}

	/**
	 * Returns the time left to the end of the playback, expressed in seconds.
	 * 
	 * @return the time left to the end in microseconds
	 * 
	 * @see #getDuration()
	 * @see #getMinutesLeft()
	 * @see #getPosition()
	 * @see #getSecondsLeft()
	 * 
	 * @see Sequencer#getMicrosecondPosition()
	 */
	public int getTimeLeft() {
		return actualDuration - getPosition();
	}

	/**
	 * Returns the total of minutes of the sequence.
	 * 
	 * @return the total of minutes of the sequence
	 * 
	 * @see #getDuration()
	 * @see #getTotalSeconds()
	 */
	public int getTotalMinutes() {
		return actualTotalMinutes;
	}

	/**
	 * Returns the total of seconds (besides minutes) of the sequence.
	 * 
	 * @return the total of seconds of the sequence
	 * 
	 * @see #getDuration()
	 * @see #getTotalMinutes()
	 */
	public int getTotalSeconds() {
		return actualTotalSeconds;
	}

	/**
	 * Returns the list of tracks that compose the sequence played by the
	 * player. Those tracks do not correspond to all of the sequence's tracks,
	 * but just the ones that in fact produce sound (there are tracks that serve
	 * only for control purposes).
	 * 
	 * @return the list of tracks that compose the sequence
	 */
	public List<Track> getTracks() {
		return tracks;
	}

	/**
	 * Returns the player's current volume, an integer in the range of 0 to 100.
	 * 
	 * @return the current volume of the player
	 * 
	 * @see #setVolume(int)
	 */
	public int getVolume() {
		return volume;
	}

	/**
	 * Sets the current position in the sequence, expressed in seconds. The new
	 * position in terms of MIDI ticks is affected by the tempo adjustment.
	 * 
	 * <p>
	 * While in loop, the player refuses to move to a position before the loop
	 * start point or after the loop end point, if the user asks for it.
	 * Instead, the player will go to the loop start point and resume playback
	 * from there.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * change in position through a call to its implementation of the
	 * <code>onPositionChange</code> method.
	 * </p>
	 * 
	 * @param seconds
	 *            desired position in seconds
	 * 
	 * @see #getPosition()
	 * @see #goTo(int, int)
	 * @see MidiPlayerListener#onPositionChange(MidiPlayer)
	 * 
	 * @see Sequencer#setMicrosecondPosition(long)
	 */
	public void goTo(int seconds) {
		sequencer
				.setMicrosecondPosition((long) (seconds * ONE_MILLION * tempoFactor));
		checkIfOutOfTheLoopRange();
		for (MidiPlayerListener listener : listeners)
			listener.onPositionChange(this);
	}

	/**
	 * Sets the current position in the sequence, expressed in minutes and
	 * seconds. The new position in terms of MIDI ticks is affected by the tempo
	 * adjustment.
	 * 
	 * <p>
	 * If in loop, the player will refuse to move to a position out of the range
	 * of the loop start point to the loop end point, going to the loop start
	 * point.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * change in position through a call to its implementation of the
	 * <code>onPositionChange</code> method.
	 * </p>
	 * 
	 * @param minutes
	 *            minutes of the desired position
	 * @param seconds
	 *            seconds of the desired position
	 * 
	 * @see #getPosition()
	 * @see #goTo(int)
	 * 
	 * @see MidiPlayerListener#onPositionChange(MidiPlayer)
	 */
	public void goTo(int minutes, int seconds) {
		goTo(minutes * 60 + seconds);
	}

	/**
	 * Obtains the current loop state for the player. By default, looping is
	 * disabled.
	 * 
	 * @return <code>true</code> if looping is enabled, <code>false</code> if
	 *         not.
	 * 
	 * @see #getLoopEndPoint()
	 * @see #getLoopStartPoint()
	 * @see #setLoop(boolean)
	 */
	public boolean isLoopEnabled() {
		return loop;
	}

	/**
	 * Obtains the current mute state for the player. The default mute state for
	 * the player which have not been muted is false. In any case where the
	 * player has not been muted, this method should return false.
	 * 
	 * @return <code>true</code> if muted, <code>false</code> if not.
	 * 
	 * @see #setMute(boolean)
	 */
	public boolean isMute() {
		return mute;
	}

	/**
	 * Determines whether the player is paused. The player is initialized
	 * stopped. In any case where the player has not been paused (by a call to
	 * the <code>pause()</code> method), this method should return false.
	 * 
	 * @return <code>true</code> if the player is paused, <code>false</code>
	 *         otherwise
	 * 
	 * @see #isPlaying()
	 * @see #isStopped()
	 * @see #pause()
	 */
	public boolean isPaused() {
		return (status == Status.PAUSED);
	}

	/**
	 * Determines whether the player is playing (not paused nor stopped). The
	 * player is initialized stopped. In any case where the player has not
	 * started playing (by a call to the <code>play()</code> method), this
	 * method should return false.
	 * 
	 * @return <code>true</code> if the player is playing (not paused nor
	 *         stopped), <code>false</code> otherwise
	 * 
	 * @see #isPaused()
	 * @see #isStopped()
	 * @see #play()
	 */
	public boolean isPlaying() {
		return (status == Status.PLAYING);
	}

	/**
	 * Determines whether the player is stopped. The player is initialized
	 * stopped. In any case where the player has not started playing (by a call
	 * to the <code>play()</code> method), this method should return false.
	 * 
	 * @return <code>true</code> if the player is stopped, <code>false</code>
	 *         otherwise
	 * 
	 * @see #isPaused()
	 * @see #isPlaying()
	 * @see #stop()
	 */
	public boolean isStopped() {
		return (status == Status.STOPPED);
	}

	/**
	 * Pauses the sequence playback.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * playback pause through a call to its implementation of the
	 * <code>onPause</code> method.
	 * </p>
	 * 
	 * @see #isPaused()
	 * @see #play()
	 * @see #stop()
	 * @see MidiPlayerListener#onPause(MidiPlayer)
	 * 
	 * @see Sequencer#stop()
	 */
	public void pause() {
		sequencer.stop();
		status = Status.PAUSED;
		for (MidiPlayerListener listener : listeners)
			listener.onPause(this);
	}

	/**
	 * Starts or resumes the sequence playback.
	 * 
	 * <p>
	 * While in loop, the player refuses to resume playback in a position before
	 * the loop start point or after the loop end point. Instead, the player
	 * will go to the loop start point and resume playback from there.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * playback start (or resume) through a call to its implementation of the
	 * <code>onStartPlaying</code> (or <code>onResumePlaying</code>) method.
	 * </p>
	 * 
	 * @see #isPlaying()
	 * @see #pause()
	 * @see #stop()
	 * @see MidiPlayerListener#onResumePlaying(MidiPlayer)
	 * @see MidiPlayerListener#onStartPlaying(MidiPlayer)
	 * 
	 * @see Sequencer#start()
	 */
	public void play() {
		checkIfOutOfTheLoopRange();
		sequencer.start();
		Status oldStatus = status;
		status = Status.PLAYING;
		if (oldStatus == Status.PAUSED) {
			for (MidiPlayerListener listener : listeners)
				listener.onResumePlaying(this);
		} else if (oldStatus == Status.STOPPED) {
			for (MidiPlayerListener listener : listeners)
				listener.onStartPlaying(this);
		}
	}

	/**
	 * Removes a <code>MidiPlayerListener</code> from the player.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * 
	 * @see #addMidiPlayerListener(MidiPlayerListener)
	 */
	public void removeMidiPlayerListener(MidiPlayerListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Sets the volume for the specified channel. The actual volume of the
	 * channel is affected by the global volume.
	 * 
	 * @param channel
	 *            the channel number. It must be an integer in the range of 0 to
	 *            15.
	 * @param volume
	 *            the new volume for the channel. It must be an integer in the
	 *            range of 0 to 100.
	 * @exception OutOfRangeException
	 *                if the specified channel is not an integer in the range of
	 *                0 to 15 or if the specified volume is not an integer in
	 *                the range of 0 to 100
	 * 
	 * @see #setVolume(int)
	 * @see VolumeController#setChannelVolume(int, int)
	 */
	protected void setChannelVolume(int channel, int volume)
			throws OutOfRangeException {
		volumeController.setChannelVolume(channel, volume);
	}

	/**
	 * Sets the loop state for the player. If looping is enabled, playback will
	 * jump to the loop start point when reaching the loop end point.
	 * 
	 * <p>
	 * While in loop, the player refuses to move to a position before the loop
	 * start point or after the loop end point, if the user asks for it.
	 * Instead, the player will go to the loop start point and resume playback
	 * from there.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * loop state change through a call to its implementation of the
	 * <code>onLoopStateChange</code> method.
	 * </p>
	 * 
	 * @param loop
	 *            the new loop state for the player. <code>true</code> implies
	 *            the playback should loop, <code>false</code> implies the
	 *            playback should continue until the end of the sequence.
	 * 
	 * @see #isLoopEnabled()
	 * @see #setLoopEndPoint(int)
	 * @see #setLoopStartPoint(int)
	 * @see MidiPlayerListener#onLoopStateChange(MidiPlayer)
	 */
	public void setLoop(boolean loop) {
		if (loop)
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
		else
			sequencer.setLoopCount(0);
		this.loop = loop;
		for (MidiPlayerListener listener : listeners)
			listener.onLoopStateChange(this);
	}

	/**
	 * Sets the last bar that will be played in the loop. If looping is not
	 * enabled, the loop end point has no effect and playback continues to play
	 * when reaching the loop end point.
	 * 
	 * <p>
	 * While in loop, the player refuses to move to a position after the loop
	 * end point, if the user asks for it. Instead, the player will go to the
	 * loop start point and resume playback from there.
	 * </p>
	 * 
	 * <p>
	 * The ending point must be greater than or equal to the starting point, and
	 * it must fall within the size of the loaded sequence. A player's loop end
	 * point defaults to the end of the sequence.
	 * </p>
	 * 
	 * <p>
	 * If the sequence's timecode is expressed in SMPTE, a call to this method
	 * has no effect, as the player does not support looping MIDI sequences with
	 * SMPTE-based timing.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * loop end point change through a call to its implementation of the
	 * <code>onLoopEndPointChange</code> method.
	 * </p>
	 * 
	 * @param barNumber
	 *            the loop's ending position, in bars (starting with 1)
	 * @exception OutOfRangeException
	 *                if the requested loop end point cannot be set, usually
	 *                because it falls outside the sequence's duration or
	 *                because the ending point is before the starting point
	 * 
	 * @see #isLoopEnabled()
	 * @see #setLoop(boolean)
	 * @see #setLoopStartPoint(int)
	 * @see MidiPlayerListener#onLoopEndPointChange(MidiPlayer)
	 * 
	 * @see Sequencer#setLoopEndPoint(long)
	 */
	public void setLoopEndPoint(int barNumber) throws OutOfRangeException {
		if (ticksPerBar != 0)
			if ((barNumber >= loopStartPoint) && (barNumber <= totalBars)) {
				loopEndPoint = barNumber;
				long loopEndPointInTicks = barNumber * ticksPerBar;
				if (loopEndPointInTicks > sequencer.getTickLength())
					loopEndPointInTicks = sequencer.getTickLength();
				sequencer.setLoopEndPoint(loopEndPointInTicks);
				checkIfOutOfTheLoopRange();
				for (MidiPlayerListener listener : listeners)
					listener.onLoopEndPointChange(this);
			} else
				throw new OutOfRangeException(barNumber, loopStartPoint,
						totalBars);
	}

	/**
	 * Sets the first bar that will be played in the loop. If looping is
	 * enabled, playback will jump to this point when reaching the loop end
	 * point.
	 * 
	 * <p>
	 * While in loop, the player refuses to move to a position before the loop
	 * start point, if the user asks for it. Instead, the player will go to the
	 * loop start point and resume playback from there.
	 * </p>
	 * 
	 * <p>
	 * A value of 1 for the starting point means the beginning of the loaded
	 * sequence. The starting point must be lower than or equal to the ending
	 * point, and it must fall within the size of the loaded sequence. A
	 * player's loop start point defaults to start of the sequence.
	 * </p>
	 * 
	 * <p>
	 * If the sequence's timecode is expressed in SMPTE, a call to this method
	 * has no effect, as the player does not support looping MIDI sequences with
	 * SMPTE-based timing.
	 * </p>
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * loop start point change through a call to its implementation of the
	 * <code>onLoopStartPointChange</code> method.
	 * </p>
	 * 
	 * @param barNumber
	 *            the loop's starting position, in bars (starting with 1)
	 * @exception OutOfRangeException
	 *                if the requested loop start point cannot be set, usually
	 *                because it falls outside the sequence's duration or
	 *                because the start point is after the end point
	 * 
	 * @see #isLoopEnabled()
	 * @see #setLoop(boolean)
	 * @see #setLoopEndPoint(int)
	 * @see MidiPlayerListener#onLoopStartPointChange(MidiPlayer)
	 * 
	 * @see Sequencer#setLoopStartPoint(long)
	 */
	public void setLoopStartPoint(int barNumber) throws OutOfRangeException {
		if (ticksPerBar != 0)
			if ((barNumber >= 1) && (barNumber <= loopEndPoint)) {
				loopStartPoint = barNumber;
				long loopStartPointInTicks = (barNumber - 1) * ticksPerBar;
				sequencer.setLoopStartPoint(loopStartPointInTicks);
				checkIfOutOfTheLoopRange();
				for (MidiPlayerListener listener : listeners)
					listener.onLoopStartPointChange(this);
			} else
				throw new OutOfRangeException(barNumber, 1, loopEndPoint);
	}

	/**
	 * Sets the mute state for the player.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * mute state change through a call to its implementation of the
	 * <code>onMuteStateChange</code> method.
	 * </p>
	 * 
	 * @param mute
	 *            the new mute state for the player. <code>true</code> implies
	 *            the player should be muted, <code>false</code> implies the
	 *            player should be unmuted.
	 * 
	 * @see #isMute()
	 * @see MidiPlayerListener#onMuteStateChange(MidiPlayer)
	 * 
	 * @see Sequencer#setTrackMute(int, boolean)
	 */
	public void setMute(boolean mute) {
		if (mute)
			for (int t = 0; t < tracks.size(); t++) {
				Track track = tracks.get(t);
				sequencer.setTrackMute(track.getNumber(), true);
			}
		else
			for (int t = 0; t < tracks.size(); t++) {
				Track track = tracks.get(t);
				sequencer.setTrackMute(track.getNumber(), track.isMute());
			}
		this.mute = mute;
		for (MidiPlayerListener listener : listeners)
			listener.onMuteStateChange(this);
	}

	/**
	 * Sets the pitch offset for all notes played by the player. This affects
	 * all currently sounding notes as well as subsequent ones. (For pitch bend
	 * to cease, the value needs to be reset to the center position.) The pitch
	 * range is two semitones up and down from center.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * pitch change through a call to its implementation of the
	 * <code>onPitchChange</code> method.
	 * </p>
	 * 
	 * @param pitch
	 *            the amount in semitones of pitch change. It must be an integer
	 *            in the range of -2 to 2. (0 = no bend)
	 * @exception OutOfRangeException
	 *                if the specified pitch is not an integer in the range of
	 *                -2 to 2
	 * 
	 * @see #getPitch()
	 * @see MidiPlayerListener#onPitchChange(MidiPlayer)
	 * 
	 * @see MidiChannel#setPitchBend(int)
	 */
	public void setPitch(int pitch) throws OutOfRangeException {
		int actualPitch = -1;
		switch (pitch) {
		case -2:
			actualPitch = 0;
			break;
		case -1:
			actualPitch = 4096;
			break;
		case 0:
			actualPitch = 8192;
			break;
		case +1:
			actualPitch = 12288;
			break;
		case +2:
			actualPitch = 16383;
			break;
		default:
			throw new OutOfRangeException(-2, +2);
		}
		if (actualPitch != -1) {
			for (MidiChannel channel : synthesizer.getChannels())
				channel.setPitchBend(actualPitch);
			this.pitch = pitch;
			for (MidiPlayerListener listener : listeners)
				listener.onPitchChange(this);
		}
	}

	/**
	 * Scales the player's actual playback tempo by the factor provided. The
	 * default is 100 (meaning 100%). A value of 100 represents the natural rate
	 * (the tempo specified in the sequence), 200 means twice as fast, etc.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * tempo change through a call to its implementation of the
	 * <code>onTempoChange</code> method.
	 * </p>
	 * 
	 * @param factor
	 *            the requested tempo scalar
	 * 
	 * @see #getTempoFactor()
	 * @see #getTempoInBPM()
	 * @see MidiPlayerListener#onTempoChange(MidiPlayer)
	 */
	public void setTempoFactor(int tempoFactor) {
		// TODO What would be an invalid value for a tempo factor?
		this.tempoFactor = (tempoFactor / 100.0f);
		actualDuration = (int) (originalDuration / this.tempoFactor);
		actualTotalMinutes = actualDuration / 60;
		actualTotalSeconds = actualDuration % 60;
		sequencer.setTempoFactor(this.tempoFactor);
		tempoInBPM = calculateTempoInBPM(this.tempoFactor);
		for (MidiPlayerListener listener : listeners)
			listener.onTempoChange(this);
	}

	/**
	 * Sets the volume for the player.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * volume change through a call to its implementation of the
	 * <code>onVolumeChange</code> method.
	 * </p>
	 * 
	 * @param volume
	 *            the new volume for the player. It must be an integer in the
	 *            range of 0 to 100.
	 * @exception OutOfRangeException
	 *                if the specified volume is not an integer in the range of
	 *                0 to 100
	 * 
	 * @see #isMute()
	 * @see MidiPlayerListener#onVolumeChange(MidiPlayer)
	 * @see VolumeController#setGlobalVolume(int)
	 */
	public void setVolume(int volume) throws OutOfRangeException {
		volumeController.setGlobalVolume(volume);
		this.volume = volume;
		for (MidiPlayerListener listener : listeners)
			listener.onVolumeChange(this);
	}

	/**
	 * Stops the sequence playback.
	 * 
	 * <p>
	 * Any registered <code>MidiPlayerListener</code> will be notified of the
	 * playback stop through a call to its implementation of the
	 * <code>onStop</code> method.
	 * </p>
	 * 
	 * @see #isStopped()
	 * @see #pause()
	 * @see #play()
	 * @see MidiPlayerListener#onStop(MidiPlayer)
	 * 
	 * @see Sequencer#stop()
	 */
	public void stop() {
		sequencer.stop();
		sequencer.setMicrosecondPosition(0);
		status = Status.STOPPED;
		for (MidiPlayerListener listener : listeners)
			listener.onStop(this);
	}

}