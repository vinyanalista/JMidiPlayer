package net.sf.jmidiplayer.core;

/**
 * The listener interface for receiving player events. The class that is
 * interested in processing a player event implements this interface, and the
 * object created with that class is registered with a player, using the track
 * <code>addMidiPlayerListener</code> method.
 * 
 * @see MidiPlayer
 * @see MidiPlayer#addMidiPlayerListener(MidiPlayerListener)
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 */
public interface MidiPlayerListener {

	/**
	 * Invoked when the loop end point for a player is changed.
	 */
	public void onLoopEndPointChange(MidiPlayer midiPlayer);
	
	/**
	 * Invoked when the loop start point for a player is changed.
	 */
	public void onLoopStartPointChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player is set to (or not to) loop.
	 */
	public void onLoopStateChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player enters or leaves the mute state.
	 */
	public void onMuteStateChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player pauses a MIDI sequence playback.
	 */
	public void onPause(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player pitch adjustment changes.
	 */
	public void onPitchChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player advances on a MIDI sequence playback.
	 */
	public void onPositionChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player resumes a MIDI sequence playback that was
	 * previously paused.
	 */
	public void onResumePlaying(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player starts a MIDI sequence playback (or restarts a MIDI
	 * sequence playback that was previously stopped).
	 */
	public void onStartPlaying(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player stops a MIDI sequence playback.
	 */
	public void onStop(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player tempo adjustment changes.
	 */
	public void onTempoChange(MidiPlayer midiPlayer);

	/**
	 * Invoked when a player volume adjustment changes.
	 */
	public void onVolumeChange(MidiPlayer midiPlayer);
}