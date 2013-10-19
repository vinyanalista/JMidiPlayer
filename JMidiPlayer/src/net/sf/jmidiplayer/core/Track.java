package net.sf.jmidiplayer.core;

import java.util.*;

import javax.sound.midi.Sequencer;

/**
 * A single MIDI track from a MIDI sequence. It acts as a wrapper to an instance
 * of <code>javax.sound.midi.Track</code>, which is the Java Sound API
 * representation of a MIDI track.
 * 
 * A <code>MidiPlayer</code> object has a collection of <code>Track</code>s,
 * that are
 * 
 * @see MidiPlayer
 * 
 * @see javax.sound.midi.Track
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 */
public class Track {

	/* Initial values */

	protected static final int CHANNEL_UNDEFINED = -1;
	private static final String INSTRUMENT_UNKNOWN = "Unknown";
	private static final String NAME_UNDEFINED = "Undefined";

	/* Attributes */

	protected int channel;
	private String instrument;
	private List<TrackListener> listeners;
	protected javax.sound.midi.Track midiTrack;
	private boolean mute;
	private String name;
	private int number;
	private MidiPlayer midiPlayer;
	private boolean solo;
	private int volume;

	/**
	 * Constructs a <code>Track</code> and initializes it to have the specified
	 * number and to be associated with the specified
	 * <code>javax.sound.midi.Track</code> and the specified player.
	 * 
	 * @param number
	 *            the number that identifies the track among the various tracks
	 *            that compose the MIDI sequence being played
	 * @param midiTrack
	 *            the Java Sound API representation of the MIDI track
	 * @param midiPlayer
	 *            the player which is playing the MIDI sequence the track
	 *            belongs
	 */
	Track(int number, javax.sound.midi.Track midiTrack, MidiPlayer midiPlayer) {
		channel = CHANNEL_UNDEFINED;
		instrument = INSTRUMENT_UNKNOWN;
		listeners = new ArrayList<TrackListener>();
		this.midiTrack = midiTrack;
		mute = midiPlayer.sequencer.getTrackMute(number);
		name = NAME_UNDEFINED;
		this.number = number;
		this.midiPlayer = midiPlayer;
		solo = midiPlayer.sequencer.getTrackSolo(number);
		volume = 100;
	}

	/**
	 * Adds a <code>TrackListener</code> to the track.
	 * 
	 * @param listener
	 *            the <code>TrackListener</code> to be added
	 * 
	 * @see #removeTrackListener(TrackListener)
	 */

	public void addTrackListener(TrackListener listener) {
		listeners.add(listener);
	}

	/**
	 * Returns a <code>String</code> with the track's instrument name.
	 * 
	 * @return the track's instrument name
	 * 
	 * @see #setInstrument(String)
	 */
	public String getInstrument() {
		return instrument;
	}
	
	/**
	 * Returns a <code>String</code> that names the track.
	 * 
	 * @return the track's name
	 * 
	 * @see #setName(String)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the number that identifies the track among the various tracks
	 * that compose the MIDI sequence being played
	 * 
	 * @return the number that identifies the track
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Returns the track's current volume, an integer in the range of 0 to 100.
	 * 
	 * @return the current volume of the track
	 * 
	 * @see #setVolume(int)
	 */
	public int getVolume() {
		return volume;
	}

	/**
	 * Obtains the current mute state for the track. The default mute state for
	 * all tracks which have not been muted is false. In any case where the
	 * track has not been muted, this method should return false.
	 * 
	 * @return <code>true</code> if muted, <code>false</code> if not.
	 * 
	 * @see #setMute(boolean)
	 */
	public boolean isMute() {
		return mute;
	}

	/**
	 * Obtains the current solo state for the track. The default solo state for
	 * all tracks which have not been solo'd is false. In any case where the
	 * track has not been solo'd, this method should return false.
	 * 
	 * @return <code>true</code> if solo'd, <code>false</code> if not.
	 */

	public boolean isSolo() {
		return solo;
	}

	/**
	 * Removes a <code>TrackListener</code> from the track.
	 * 
	 * @param listener
	 *            the listener to be removed
	 * 
	 * @see #addTrackListener(TrackListener)
	 */
	public void removeTrackListener(TrackListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Sets the track's instrument name.
	 * 
	 * @param instrument
	 *            the new track's instrument name
	 * 
	 * @see #getInstrument()
	 */
	public void setInstrument(String instrument) {
		this.instrument = instrument.trim();
	}
	
	/**
	 * Sets the track's name.
	 * 
	 * @param name
	 *            the new track's name
	 * 
	 * @see #getName()
	 */
	public void setName(String name) {
		this.name = name.trim();
	}

	/**
	 * Sets the mute state for the track.
	 * 
	 * <p>
	 * Any registered <code>TrackListener</code> will be notified of the mute
	 * state change through a call to its implementation of the
	 * <code>onMuteStateChange</code> method.
	 * </p>
	 * 
	 * @param mute
	 *            the new mute state for the track. <code>true</code> implies
	 *            the track should be muted, <code>false</code> implies the
	 *            track should be unmuted.
	 * 
	 * @see #isMute()
	 * @see TrackListener#onMuteStateChange(Track)
	 * 
	 * @see Sequencer#setTrackMute(int, boolean)
	 */
	public void setMute(boolean mute) {
		midiPlayer.sequencer.setTrackMute(number, mute);
		this.mute = mute;
		for (TrackListener listener : listeners)
			listener.onMuteStateChange(this);
	}

	/**
	 * Sets the solo state for a track. If <code>solo</code> is
	 * <code>true</code> only this track and other solo'd tracks will sound. If
	 * <code>solo</code> is <code>false</code> then only other solo'd tracks
	 * will sound, unless no tracks are solo'd in which case all un-muted tracks
	 * will sound.
	 * 
	 * <p>
	 * Any registered <code>TrackListener</code> will be notified of the solo
	 * state change through a call to its implementation of the
	 * <code>onSoloStateChange</code> method.
	 * </p>
	 * 
	 * @param solo
	 *            the new solo state for the track. <code>true</code> implies
	 *            the track should be solo'd, <code>false</code> implies the
	 *            track should not be solo'd.
	 * 
	 * @see #isSolo()
	 * @see TrackListener#onSoloStateChange(Track)
	 * 
	 * @see Sequencer#setTrackSolo(int, boolean)
	 */
	public void setSolo(boolean solo) {
		midiPlayer.sequencer.setTrackSolo(number, solo);
		this.solo = solo;
		for (TrackListener listener : listeners)
			listener.onSoloStateChange(this);
	}

	/**
	 * Sets the volume for the track.
	 * 
	 * <p>
	 * Any registered <code>TrackListener</code> will be notified of the volume
	 * change through a call to its implementation of the
	 * <code>onVolumeChange</code> method.
	 * </p>
	 * 
	 * @param volume
	 *            the new volume for the track. It must be an integer in the
	 *            range of 0 to 100.
	 * 
	 * @exception OutOfRangeException
	 *                if the specified volume is not an integer in the range of
	 *                0 to 100
	 * 
	 * @see #isMute()
	 * @see TrackListener#onVolumeChange(Track)
	 * 
	 * @see MidiPlayer#setChannelVolume(int, int)
	 */
	public void setVolume(int volume) throws OutOfRangeException {
		if ((volume < 0) || (volume > 100))
			throw new OutOfRangeException(0, 100);
		if (channel != -1) {
			midiPlayer.setChannelVolume(channel, volume);
			this.volume = volume;
			for (TrackListener listener : listeners)
				listener.onVolumeChange(this);
		}
	}

	@Override
	public String toString() {
		if (!name.equals(NAME_UNDEFINED))
			return name;
		else
			return instrument;
	}

}