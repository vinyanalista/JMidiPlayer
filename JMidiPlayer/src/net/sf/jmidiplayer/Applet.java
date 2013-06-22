package net.sf.jmidiplayer;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

import net.sf.jmidiplayer.gui.MidiPlayerGUI;
import net.sf.jmidiplayer.icons.Icon;
import net.sf.jmidiplayer.utils.Log;


import java.net.*;

/**
 * The JMidiPlayer application when run as an applet on a web page.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 * @see MidiPlayerGUI
 * 
 */
public class Applet extends JApplet {

	private static final long serialVersionUID = 1L;

	public Applet() {
	}

	public void init() {
		String filePath = null, title = null, error = null;
		filePath = getParameter("file");
		if (filePath == null) {
			// If file path was not set, displays an error message
			error = "The \"file\" parameter must be set.";
		} else {
			try {
				URL fileURL = new URL(getCodeBase(), filePath);
				URLConnection conn = (URLConnection) fileURL.openConnection();
				conn.setRequestProperty("REFERER", getDocumentBase().toString());
				InputStream inputStream = conn.getInputStream();
				title = getParameter("title");
				// If title was not set, the file name will be displayed instead
				if (title == null)
					title = new File(fileURL.getFile()).getName();
				MidiPlayerGUI gui = new MidiPlayerGUI(inputStream, title);
				getContentPane().add(gui);
				gui.play();
			} catch (Exception exception) {
				error = exception.getMessage();
			}
		}
		if (error != null) {
			Log.error(error);
			String html = "<html><body style='width: "
					+ (getSize().getWidth() - 100) + "px'>";
			JLabel errorLabel = new JLabel(html + error,
					Icon.get(Icon.DIALOG_ERROR), JLabel.LEFT);
			errorLabel.setForeground(Color.RED);
			add(errorLabel);
			JOptionPane.showMessageDialog(null, error, "Error",
					JOptionPane.ERROR_MESSAGE, Icon.get(Icon.DIALOG_ERROR));
		}
	}

	@Override
	public void stop() {
		super.stop();
		Log.write("Playback stopped");
	}
}