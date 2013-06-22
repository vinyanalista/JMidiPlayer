package net.sf.jmidiplayer.core;

/**
 * The listener interface for receiving track events. The class that is
 * interested in processing a track event implements this interface, and the
 * object created with that class is registered with a track, using the track
 * <code>addTrackListener</code> method.
 * 
 * @see Track
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 */
public interface TrackListener {

	/**
	 * Invoked when a track enters or leaves the mute state.
	 */
	public void onMuteStateChange(Track track);

	/**
	 * Invoked when a track enters or leaves the solo state.
	 */
	public void onSoloStateChange(Track track);

	/**
	 * Invoked when a track volume adjustment changes.
	 */
	public void onVolumeChange(Track track);
}