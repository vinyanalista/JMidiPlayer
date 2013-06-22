package net.sf.jmidiplayer.core;

import javax.sound.midi.*;

/**
 * A receiver that can intercept MIDI note on messages sent from a transmitter
 * to a receiver and add volume information to those messages. A
 * <code>VolumeController</code> can adjust the volume of the played notes as a
 * whole and/or on a channel basis.
 * 
 * <p>
 * Actually, the volume adjustment is made by changing the note's velocity (i.e.
 * the pressure put on a piano keyboard to play it).
 * </p>
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 * @see Receiver
 * @see Transmitter
 * 
 */
public class VolumeController implements Receiver {

	/* Attributes */

	private float[] actualChannelVolumes;
	private float[] channelVolumes;
	private float globalVolume;
	private Receiver receiver;

	/**
	 * Constructs a <code>VolumeController</code> that will receive MIDI note on
	 * messages, recalculate the volumes of the notes and send the modified
	 * messages to the specified receiver.
	 * 
	 * <p>
	 * It is important to detach the specified receiver from its original
	 * transmitter and then insert the new <code>VolumeController</code> between
	 * them, such as in:
	 * </p>
	 * 
	 * <p>
	 * <code>for (Transmitter transmitter : sequencer.getTransmitters())<br>
				transmitter.close();<br>
			sequencer.getTransmitter().setReceiver(volumeController);</code>
	 * </p>
	 * 
	 * @param receiver
	 *            the receiver to which the note with the recalculated volume
	 *            should be sent
	 */
	public VolumeController(Receiver receiver) {
		globalVolume = 1.0f;
		channelVolumes = new float[16];
		actualChannelVolumes = new float[16];
		for (int channel = 0; channel < 16; channel++) {
			channelVolumes[channel] = 1.0f;
			actualChannelVolumes[channel] = 1.0f;
		}
		this.receiver = receiver;
	}

	/**
	 * Returns a new volume for a single note, based on the channel it came
	 * from, the note's original volume and the global volume.
	 * 
	 * @param channel
	 *            the number of the channel from which the note came from. It
	 *            must be an integer in the range of 0 to 15.
	 * @param originalVolume
	 *            the original volume (velocity) of the note
	 * @return the new volume for the note
	 */
	private int calculateNewVolume(int channel, int originalVolume) {
		return (int) (originalVolume * actualChannelVolumes[channel]);
	}

	@Override
	public void close() {
		receiver.close();
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage) {
			ShortMessage shortMessage = (ShortMessage) message;
			if (shortMessage.getCommand() == 0x90) {
				try {
					shortMessage.setMessage(
							shortMessage.getStatus(),
							shortMessage.getData1(),
							calculateNewVolume(shortMessage.getChannel(),
									shortMessage.getData2()));
				} catch (InvalidMidiDataException e) {
					e.printStackTrace();
				}
			}
		}
		receiver.send(message, timeStamp);
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
	 * @see #setGlobalVolume(int)
	 */
	public void setChannelVolume(int channel, int volume)
			throws OutOfRangeException {
		if ((channel < 0) || (channel > 15))
			throw new OutOfRangeException(0, 15);
		if ((volume < 0) || (volume > 100))
			throw new OutOfRangeException(0, 100);
		if ((channel >= 0) && (channel < 16)) {
			if (volume < 0)
				volume = 0;
			else if (volume > 100)
				volume = 100;
			channelVolumes[channel] = (volume / 100.f);
			actualChannelVolumes[channel] = channelVolumes[channel]
					* globalVolume;
		}
	}

	/**
	 * Sets the global volume.
	 * 
	 * @param volume
	 *            the global volume. It must be an integer in the range of 0 to
	 *            100.
	 * @exception OutOfRangeException
	 *                if the specified volume is not an integer in the range of
	 *                0 to 100
	 */
	public void setGlobalVolume(int volume) throws OutOfRangeException {
		if ((volume < 0) || (volume > 100))
			throw new OutOfRangeException(0, 100);
		if (volume < 0)
			volume = 0;
		else if (volume > 100)
			volume = 100;
		globalVolume = (volume / 100.f);
		for (int channel = 0; channel < 16; channel++)
			actualChannelVolumes[channel] = channelVolumes[channel]
					* globalVolume;
	}

}