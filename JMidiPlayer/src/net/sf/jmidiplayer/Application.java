package net.sf.jmidiplayer;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import net.sf.jmidiplayer.gui.MidiPlayerGUI;
import net.sf.jmidiplayer.icons.Icon;
import net.sf.jmidiplayer.utils.Log;

/**
 * The JMidiPlayer application when run as a standalone application.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 * @see MidiPlayerGUI
 */
public class Application extends JFrame {

	private static final long serialVersionUID = 1L;

	public Application(String filePath, String title)
			throws FileNotFoundException {
		super("JMidiPlayer");
		MidiPlayerGUI gui = new MidiPlayerGUI(new FileInputStream(new File(
				filePath)), title);
		add(gui);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setIconImage(Icon.getAsImage(Icon.MIDI_PLAYER));
		setMinimumSize(gui.getMinimumSize());
		setSize(500, 450);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Log.write("Playback stopped");
			}
		});
		gui.play();
	}

	public static void main(String[] args) {
		String filePath = null, title = null;
		if (args.length < 3) {
			if (args.length == 0) {
				// No arguments: should open a dialog for choosing the file
				JFileChooser openFile = new JFileChooser();
				openFile.setFileFilter(new FileFilter() {
					@Override
					public String getDescription() {
						return "MIDI files (.mid, .midi)";
					}

					@Override
					public boolean accept(File f) {
						return (f.getPath().contains(".mid")
								|| f.getPath().contains(".midi") || f
								.isDirectory());
					}
				});
				openFile.setFileView(new FileView() {
					public String getTypeDescription(File f) {
						if (f.getPath().contains(".mid")
								|| f.getPath().contains(".midi")) {
							return "MIDI file";
						} else if (f.isDirectory())
							return "Folder";
						else
							return null;
					}

					public javax.swing.Icon getIcon(File f) {
						if (f.getPath().contains(".mid")
								|| f.getPath().contains(".midi")) {
							return Icon.get(Icon.MIDI_FILE);
						} else if (f.isDirectory())
							return Icon.get(Icon.FOLDER);
						else
							return null;
					}
				});
				openFile.setCurrentDirectory(new File(System
						.getProperty("user.home")));
				if (openFile.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					filePath = openFile.getSelectedFile().getAbsolutePath();
				} else
					System.exit(0);
			} else {
				// The first argument (not mandatory) is the file path
				if ((args.length >= 1) && (args[0].trim() != "")) {
					filePath = args[0].trim();

				}
				// The second argument (not mandatory) is the title
				if (args.length == 2) {
					title = args[1].trim();
				}
			}
			try {
				// If title was not set, the file name will be displayed instead
				if (title == null)
					title = new File(filePath).getName();
				new Application(filePath, title);
			} catch (Exception exception) {
				JOptionPane.showMessageDialog(null, "File not found!", "Error",
						JOptionPane.ERROR_MESSAGE, Icon.get(Icon.DIALOG_ERROR));
				Log.error(exception.getMessage());
			}
		} else {
			System.out
					.println("Usage: java -jar JMidiPlayer.jar [path_to_the_midi_file.mid] [title]");
		}
	}
}