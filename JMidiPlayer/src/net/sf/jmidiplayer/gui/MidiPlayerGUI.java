package net.sf.jmidiplayer.gui;

import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

import net.sf.jmidiplayer.core.*;
import net.sf.jmidiplayer.icons.Icon;
import net.sf.jmidiplayer.utils.Log;

/**
 * The JMidiPlayer GUI, that is the same to both the standalone application and
 * the applet.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 * @see MidiPlayer
 */
public class MidiPlayerGUI extends JPanel implements MidiPlayerListener,
		TrackListener {

	protected static final long serialVersionUID = 1L;

	/* Constants */

	private static final int BORDER = 5;
	private static final int BUTTON_HEIGHT = 28;

	/* LED configuration */

	private static final String FONT_LED = "/net/sf/jmidiplayer/fonts/digital-7.ttf";
	private static final int FONT_LED_SIZE = 18;
	private static final Color FONT_LED_COLOR = new Color(232, 244, 255);
	private static final Color FONT_LED_BACKGROUND = new Color(25, 59, 123);

	/* Components */

	private MidiPlayer midiPlayer;
	private String title;
	private JLabel timeLabel, remainingLabel, totalLabel, tempoLabel;
	private JButton playPauseButton, stopButton, muteButton;
	private JSlider positionSlider, volumeSlider, tempoSlider, pitchSlider;
	private HashMap<Track, JButton> soloTrackButtons, muteTrackButtons;
	private HashMap<Track, JSlider> trackVolumeSliders;
	private HashMap<Track, JSpinner> trackVolumeSpinners;
	private JSpinner volumeSpinner, loopStartSpinner, loopEndSpinner,
			tempoSpinner, pitchSpinner;
	private Timer timer;

	public MidiPlayerGUI(InputStream midiFile, String title) {
		try {
			midiPlayer = new MidiPlayer(midiFile);
			this.title = title;
			midiPlayer.setLoop(true);
			midiPlayer.addMidiPlayerListener(this);
			initComponents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Preparing the GUI */

	protected void setSize(JComponent component, int width, int height) {
		Dimension size = new Dimension(width, height);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setPreferredSize(size);
	}

	protected void initComponents() throws Exception {

		/* Title */
		JLabel titleLabel = new JLabel(title, JLabel.CENTER);
		titleLabel.setAlignmentX(CENTER_ALIGNMENT);
		titleLabel.setAlignmentY(CENTER_ALIGNMENT);
		titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
		titleLabel.setForeground(FONT_LED_COLOR);

		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
				BORDER, BORDER));
		titlePanel.setMaximumSize(new Dimension(10000, 30));
		titlePanel.setPreferredSize(new Dimension(600, 30));
		titlePanel.setMinimumSize(new Dimension(200, 30));
		titlePanel.setBackground(FONT_LED_BACKGROUND);
		titlePanel.add(titleLabel);

		/* Loading the font used by the information panels */

		Font ledFont = Font.createFont(Font.TRUETYPE_FONT, getClass()
				.getResourceAsStream(FONT_LED));
		try {
			InputStream fontStream = getClass().getResourceAsStream(FONT_LED);
			ledFont = Font.createFont(Font.TRUETYPE_FONT, fontStream)
					.deriveFont(Font.PLAIN, FONT_LED_SIZE);
			fontStream.close();
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		/* Time played so far */

		timeLabel = new JLabel("00:00", JLabel.CENTER);
		timeLabel.setFont(ledFont);
		timeLabel.setForeground(FONT_LED_COLOR);

		/* Remaining time to be played */

		remainingLabel = new JLabel(formatTime(midiPlayer.getTotalMinutes(),
				midiPlayer.getTotalSeconds()), JLabel.CENTER);
		remainingLabel.setFont(ledFont);
		remainingLabel.setForeground(FONT_LED_COLOR);

		/* Duration of the song */

		totalLabel = new JLabel(remainingLabel.getText(), JLabel.CENTER);
		totalLabel.setFont(ledFont);
		totalLabel.setForeground(FONT_LED_COLOR);

		/* Tempo in BPM */

		tempoLabel = new JLabel(String.valueOf(midiPlayer.getTempoInBPM()),
				JLabel.CENTER);
		tempoLabel.setFont(ledFont);
		tempoLabel.setForeground(FONT_LED_COLOR);

		/* Information panel */

		JLabel timeLabel2 = new JLabel("TIME", JLabel.CENTER);
		timeLabel2.setForeground(FONT_LED_COLOR);

		JLabel remainingLabel2 = new JLabel("REMAINING", JLabel.CENTER);
		remainingLabel2.setForeground(FONT_LED_COLOR);

		JLabel totalLabel2 = new JLabel("TOTAL", JLabel.CENTER);
		totalLabel2.setForeground(FONT_LED_COLOR);

		JLabel tempoLabel2 = new JLabel("BPM", JLabel.CENTER);
		tempoLabel2.setForeground(FONT_LED_COLOR);

		JPanel timePanel = new JPanel(new GridLayout(2, 1, BORDER, BORDER));
		timePanel.setBackground(FONT_LED_BACKGROUND);
		timePanel.add(timeLabel);
		timePanel.add(timeLabel2);

		JPanel remainingPanel = new JPanel(new GridLayout(2, 1, BORDER, BORDER));
		remainingPanel.setBackground(FONT_LED_BACKGROUND);
		remainingPanel.add(remainingLabel);
		remainingPanel.add(remainingLabel2);

		JPanel totalPanel = new JPanel(new GridLayout(2, 1, BORDER, BORDER));
		totalPanel.setBackground(FONT_LED_BACKGROUND);
		totalPanel.add(totalLabel);
		totalPanel.add(totalLabel2);

		JPanel tempoPanel = new JPanel(new GridLayout(2, 1, BORDER, BORDER));
		tempoPanel.setBackground(FONT_LED_BACKGROUND);
		tempoPanel.add(tempoLabel);
		tempoPanel.add(tempoLabel2);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20,
				BORDER));
		infoPanel.setMaximumSize(new Dimension(10000, 50));
		infoPanel.setPreferredSize(new Dimension(600, 50));
		infoPanel.setMinimumSize(new Dimension(200, 50));
		infoPanel.setBackground(FONT_LED_BACKGROUND);
		infoPanel.add(timePanel);
		infoPanel.add(remainingPanel);
		infoPanel.add(totalPanel);
		infoPanel.add(tempoPanel);

		/* Play/pause button */

		playPauseButton = new JButton();
		setSize(playPauseButton, BUTTON_HEIGHT, BUTTON_HEIGHT);
		setPlayPauseButton(true);
		playPauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				playPauseButtonClicked();
			}
		});

		/* Stop button */

		stopButton = new JButton();
		setSize(stopButton, BUTTON_HEIGHT, BUTTON_HEIGHT);
		stopButton.setIcon(Icon.get(Icon.STOP));
		stopButton.setToolTipText("Stop");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				stopButtonClicked();
			}
		});

		/* Position slider */

		positionSlider = new JSlider(0, midiPlayer.getDuration());
		positionSlider.setToolTipText("Position");
		positionSlider.setValue(0);
		positionSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				positionSliderChanged();
			}
		});

		/* Playback control box */

		Box playbackControlBox = Box.createHorizontalBox();
		playbackControlBox.add(playPauseButton);
		playbackControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		playbackControlBox.add(stopButton);
		playbackControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		playbackControlBox.add(positionSlider);

		/* Mute/unmute button */

		muteButton = new JButton(Icon.get(Icon.VOLUME_HIGH));
		setSize(muteButton, BUTTON_HEIGHT, BUTTON_HEIGHT);
		muteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				muteButtonClicked();
			}
		});

		/* Volume slider */

		volumeSlider = new JSlider(0, 100);
		volumeSlider.setValue(100);
		volumeSlider.setToolTipText("Volume");
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				volumeSliderChanged();
			}
		});

		/* Volume spinner */

		volumeSpinner = newPercentSpinner();
		setSize(volumeSpinner, 50, BUTTON_HEIGHT);
		volumeSpinner.setToolTipText("Volume");
		volumeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				volumeSpinnerChanged();
			}
		});

		/* Volume control box */

		Box volumeControlBox = Box.createHorizontalBox();
		volumeControlBox.add(muteButton);
		volumeControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		volumeControlBox.add(volumeSlider);
		volumeControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		volumeControlBox.add(volumeSpinner);

		Box innerLoopControlBox = Box.createHorizontalBox();

		if (midiPlayer.getNumberOfBars() != 0) {

			/* Loop start spinner */

			loopStartSpinner = new JSpinner(new SpinnerNumberModel(1, 1,
					midiPlayer.getNumberOfBars(), 1));
			setSize(loopStartSpinner, 50, BUTTON_HEIGHT);
			loopStartSpinner.setToolTipText("Loop start bar");
			loopStartSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent event) {
					loopStartSpinnerChanged();
				}
			});

			/* Loop end spinner */

			loopEndSpinner = new JSpinner(new SpinnerNumberModel(
					midiPlayer.getNumberOfBars(), 1,
					midiPlayer.getNumberOfBars(), 1));
			setSize(loopEndSpinner, 50, BUTTON_HEIGHT);
			loopEndSpinner.setToolTipText("Loop end bar");
			loopEndSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent event) {
					loopEndSpinnerChanged();
				}
			});

			/* Loop control box */

			JLabel loopIcon = new JLabel(Icon.get(Icon.LOOP));

			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(loopIcon);
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(new JLabel("Start bar:"));
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(loopStartSpinner);
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(new JLabel("End bar:"));
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(loopEndSpinner);
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
		} else {

			/* Warning */

			JLabel smpteWarning = new JLabel(
					"SMPTE-based timing detected! Looping was disabled.",
					Icon.get(Icon.ERROR), JLabel.LEFT);
			smpteWarning.setForeground(Color.RED);
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
			innerLoopControlBox.add(smpteWarning);
			innerLoopControlBox.add(Box
					.createRigidArea(new Dimension(BORDER, 0)));
		}

		Box loopControlBox = Box.createVerticalBox();
		loopControlBox.setBorder(new TitledBorder("Loop"));
		loopControlBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		loopControlBox.add(innerLoopControlBox);
		loopControlBox.add(Box.createRigidArea(new Dimension(0, BORDER)));

		/* Tempo slider */

		tempoSlider = new JSlider(50, 200, 100);
		tempoSlider.setToolTipText("Tempo");
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(new Integer(50), new JLabel("50%"));
		labels.put(new Integer(100), new JLabel("100%"));
		labels.put(new Integer(200), new JLabel("200%"));
		tempoSlider.setLabelTable(labels);
		tempoSlider.setPaintLabels(true);
		tempoSlider.setMajorTickSpacing(50);
		tempoSlider.setPaintTicks(true);
		tempoSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				tempoSliderChanged();
			}
		});

		/* Tempo spinner */

		tempoSpinner = new JSpinner(new SpinnerNumberModel(100, 50, 200, 1));
		setSize(tempoSpinner, 50, BUTTON_HEIGHT);
		tempoSpinner.setToolTipText("Tempo");
		tempoSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				tempoSpinnerChanged();
			}
		});

		/* Tempo control box */

		JLabel tempoIcon = new JLabel(Icon.get(Icon.TEMPO));

		Box innerTempoControlBox = Box.createHorizontalBox();
		innerTempoControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerTempoControlBox.add(tempoIcon);
		innerTempoControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerTempoControlBox.add(tempoSlider);
		innerTempoControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerTempoControlBox.add(tempoSpinner);
		innerTempoControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));

		Box tempoControlBox = Box.createVerticalBox();
		tempoControlBox.setBorder(new TitledBorder("Tempo"));
		tempoControlBox.add(innerTempoControlBox);
		tempoControlBox.add(Box.createRigidArea(new Dimension(0, BORDER)));

		/* Pitch slider */

		pitchSlider = new JSlider(-2, +2, 0);
		pitchSlider.setToolTipText("Pitch");
		labels = new Hashtable<Integer, JLabel>();
		labels.put(new Integer(-2), new JLabel("-2"));
		labels.put(new Integer(-1), new JLabel("-1"));
		labels.put(new Integer(0), new JLabel("0"));
		labels.put(new Integer(+1), new JLabel("+1"));
		labels.put(new Integer(+2), new JLabel("+2"));
		pitchSlider.setLabelTable(labels);
		pitchSlider.setPaintLabels(true);
		pitchSlider.setMajorTickSpacing(1);
		pitchSlider.setPaintTicks(true);
		pitchSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				pitchSliderChanged();
			}
		});

		/* Pitch spinner */

		pitchSpinner = new JSpinner(new SpinnerNumberModel(0, -2, +2, 1));
		setSize(pitchSpinner, 60, BUTTON_HEIGHT);
		pitchSpinner.setToolTipText("Pitch");
		pitchSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				pitchSpinnerChanged();
			}
		});

		/* Pitch control box */

		JLabel pitchIcon = new JLabel(Icon.get(Icon.PITCH));

		Box innerPitchControlBox = Box.createHorizontalBox();
		innerPitchControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerPitchControlBox.add(pitchIcon);
		innerPitchControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerPitchControlBox.add(pitchSlider);
		innerPitchControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		innerPitchControlBox.add(pitchSpinner);
		innerPitchControlBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));

		Box pitchControlBox = Box.createVerticalBox();
		pitchControlBox.setBorder(new TitledBorder("Pitch"));
		pitchControlBox.add(innerPitchControlBox);
		pitchControlBox.add(Box.createRigidArea(new Dimension(0, BORDER)));

		/* Tempo and pitch controls go side by side */

		Box tempoAndPitchBox = Box.createHorizontalBox();
		tempoAndPitchBox.add(tempoControlBox);
		tempoAndPitchBox.add(Box.createRigidArea(new Dimension(BORDER, 0)));
		tempoAndPitchBox.add(pitchControlBox);

		/* Track control */

		int numberOfTracks = midiPlayer.getTracks().size();

		double columns[] = { BORDER, TableLayout.PREFERRED, BORDER,
				BUTTON_HEIGHT, BORDER, BUTTON_HEIGHT, BORDER, TableLayout.FILL,
				BORDER, 50, BORDER };
		double rows[] = new double[(numberOfTracks * 2) + 1];
		rows[0] = BORDER;
		rows[rows.length - 1] = BORDER;
		for (int r = 1; r < rows.length - 1; r++) {
			if (r % 2 == 1)
				rows[r] = BUTTON_HEIGHT;
			else
				rows[r] = BORDER;
		}
		JPanel trackControlPanel = new JPanel(new TableLayout(new double[][] {
				columns, rows }));

		soloTrackButtons = new HashMap<Track, JButton>(numberOfTracks);
		muteTrackButtons = new HashMap<Track, JButton>(numberOfTracks);
		trackVolumeSliders = new HashMap<Track, JSlider>(numberOfTracks);
		trackVolumeSpinners = new HashMap<Track, JSpinner>(numberOfTracks);

		// for (int t = 0; t < numberOfTracks; t++) {
		// final Track track = player.getTrack(t);
		int t = 0;
		for (final Track track : midiPlayer.getTracks()) {
			track.addTrackListener(this);

			/* Solo button for the track */

			JButton soloTrackButton = new JButton();
			setSize(soloTrackButton, BUTTON_HEIGHT, BUTTON_HEIGHT);
			soloTrackButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					soloTrackButtonClicked(track);
				}
			});
			soloTrackButtons.put(track, soloTrackButton);
			setTrackSoloButton(track);

			/* Mute button for the track */

			JButton muteTrackButton = new JButton(Icon.get(Icon.VOLUME_HIGH));
			setSize(muteTrackButton, BUTTON_HEIGHT, BUTTON_HEIGHT);
			muteTrackButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					muteTrackButtonClicked(track);
				}
			});
			muteTrackButtons.put(track, muteTrackButton);

			/* Volume slider for the track */

			JSlider trackVolumeSlider = new JSlider(0, 100);
			trackVolumeSlider.setValue(track.getVolume());
			trackVolumeSlider.setMinimumSize(new Dimension(10, 20));
			trackVolumeSlider.setPreferredSize(new Dimension(50, 20));
			trackVolumeSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent event) {
					trackVolumeSliderChanged(track);
				}
			});
			trackVolumeSliders.put(track, trackVolumeSlider);
			trackVolumeSlider.setToolTipText("Volume of " + track);

			/* Volume spinner for the track */

			JSpinner trackVolumeSpinner = newPercentSpinner();
			setSize(trackVolumeSpinner, 50, BUTTON_HEIGHT);
			trackVolumeSpinner.setToolTipText("Volume of " + track);
			trackVolumeSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent event) {
					trackVolumeSpinnerChanged(track);
				}
			});
			trackVolumeSpinners.put(track, trackVolumeSpinner);
			trackVolumeSpinner.setToolTipText("Volume of " + track);
			setTrackVolumeControls(track);

			int rowNumber = (t * 2) + 1;

			trackControlPanel.add(new JLabel(track.getDescription()), "1, "
					+ rowNumber);
			trackControlPanel.add(soloTrackButton, "3, " + rowNumber);
			trackControlPanel.add(muteTrackButton, "5, " + rowNumber);
			trackControlPanel.add(trackVolumeSlider, "7, " + rowNumber);
			trackControlPanel.add(trackVolumeSpinner, "9, " + rowNumber);

			t++;
		}

		JScrollPane trackControlScrollPane = new JScrollPane(trackControlPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		trackControlScrollPane.setBorder(new TitledBorder("Tracks"));
		trackControlScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		/* Main interface */

		Box mainBox = Box.createVerticalBox();
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(titlePanel);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(infoPanel);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(playbackControlBox);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(volumeControlBox);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(loopControlBox);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(tempoAndPitchBox);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));
		mainBox.add(trackControlScrollPane);
		mainBox.add(Box.createRigidArea(new Dimension(0, BORDER)));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setMinimumSize(new Dimension(510, 405));
		add(Box.createRigidArea(new Dimension(BORDER, 0)));
		add(mainBox);
		add(Box.createRigidArea(new Dimension(BORDER, 0)));

		/* Timer for updating the position slider */

		timer = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				timerEvent();
			}
		});
	}

	/* Interface events */

	protected void playPauseButtonClicked() {
		if (midiPlayer.isPlaying()) {
			pause();
		} else {
			play();
		}
	}

	protected void stopButtonClicked() {
		stop();
	}

	protected void positionSliderChanged() {
		int positionSliderValue = positionSlider.getValue();
		if (positionSliderValue != midiPlayer.getPosition()) {
			midiPlayer.goTo(positionSliderValue);
		}
		if (!positionSlider.getValueIsAdjusting()) {
			positionSlider.setValue(midiPlayer.getPosition());
		}
		updateTimeLabels();
	}

	protected void muteButtonClicked() {
		midiPlayer.setMute(!midiPlayer.isMute());
	}

	protected void volumeSliderChanged() {
		try {
			int volumeSliderValue = volumeSlider.getValue();
			if (volumeSliderValue != midiPlayer.getVolume())
				midiPlayer.setVolume(volumeSliderValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void volumeSpinnerChanged() {
		try {
			int volumeSpinnerValue = (Integer) volumeSpinner.getValue();
			if (volumeSpinnerValue != midiPlayer.getVolume())
				midiPlayer.setVolume(volumeSpinnerValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void loopStartSpinnerChanged() {
		try {
			int loopStartPoint = (Integer) loopStartSpinner.getValue();
			if (loopStartPoint != midiPlayer.getLoopStartPoint()) {
				midiPlayer.setLoopStartPoint(loopStartPoint);
				((SpinnerNumberModel) loopEndSpinner.getModel())
						.setMinimum(loopStartPoint);
			}
		} catch (OutOfRangeException e) {
		}
	}

	protected void loopEndSpinnerChanged() {
		try {
			int loopEndPoint = (Integer) loopEndSpinner.getValue();
			if (loopEndPoint != midiPlayer.getLoopEndPoint()) {
				midiPlayer.setLoopEndPoint(loopEndPoint);
				((SpinnerNumberModel) loopStartSpinner.getModel())
						.setMaximum(loopEndPoint);
			}
		} catch (OutOfRangeException e) {
		}
	}

	protected void tempoSliderChanged() {
		int tempoSliderValue = tempoSlider.getValue();
		if (tempoSliderValue != midiPlayer.getTempoFactor())
			midiPlayer.setTempoFactor(tempoSliderValue);
	}

	protected void tempoSpinnerChanged() {
		int tempoSpinnerValue = (Integer) tempoSpinner.getValue();
		if (tempoSpinnerValue != midiPlayer.getTempoFactor())
			midiPlayer.setTempoFactor(tempoSpinnerValue);
	}

	protected void pitchSliderChanged() {
		try {
			int pitchSliderValue = pitchSlider.getValue();
			if (pitchSliderValue != midiPlayer.getPitch())
				midiPlayer.setPitch(pitchSliderValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void pitchSpinnerChanged() {
		try {
			int pitchSpinnerValue = (Integer) pitchSpinner.getValue();
			if (pitchSpinnerValue != midiPlayer.getPitch())
				midiPlayer.setPitch(pitchSpinnerValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void soloTrackButtonClicked(Track track) {
		track.setSolo(!track.isSolo());
	}

	protected void muteTrackButtonClicked(Track track) {
		track.setMute(!track.isMute());
	}

	protected void trackVolumeSliderChanged(Track track) {
		try {
			int trackVolumeSliderValue = trackVolumeSliders.get(track)
					.getValue();
			if (trackVolumeSliderValue != track.getVolume())
				track.setVolume(trackVolumeSliderValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void trackVolumeSpinnerChanged(Track track) {
		try {
			int trackVolumeSpinnerValue = (Integer) trackVolumeSpinners.get(
					track).getValue();
			if (trackVolumeSpinnerValue != track.getVolume())
				track.setVolume(trackVolumeSpinnerValue);
		} catch (OutOfRangeException e) {
		}
	}

	protected void timerEvent() {
		if (midiPlayer.isPlaying()) {
			positionSlider.setValue(midiPlayer.getPosition());
		}
	}

	/* Player events */

	@Override
	public void onLoopEndPointChange(MidiPlayer midiPlayer) {
		Log.write("Loop end point changed to " + midiPlayer.getLoopEndPoint());
		if (positionSlider.getValue() != midiPlayer.getPosition()) {
			positionSlider.setValue(midiPlayer.getPosition());
			updateTimeLabels();
		}
	}

	@Override
	public void onLoopStartPointChange(MidiPlayer midiPlayer) {
		Log.write("Loop start point changed to "
				+ midiPlayer.getLoopStartPoint());
		if (positionSlider.getValue() != midiPlayer.getPosition()) {
			positionSlider.setValue(midiPlayer.getPosition());
			updateTimeLabels();
		}
	}

	@Override
	public void onLoopStateChange(MidiPlayer midiPlayer) {
		Log.write("Looping is activated by default");
	}

	@Override
	public void onMuteStateChange(MidiPlayer midiPlayer) {
		if (midiPlayer.isMute())
			Log.write("Player has gone mute");
		else
			Log.write("Player has gone unmute");
		setVolumeControls();
	}

	@Override
	public void onPause(MidiPlayer midiPlayer) {
		Log.write("Playback paused");
		setPlayPauseButton(true);
	}

	@Override
	public void onPitchChange(MidiPlayer midiPlayer) {
		String message = "Pitch offset set to ";
		if (midiPlayer.getPitch() < 0)
			message += midiPlayer.getPitch();
		else if (midiPlayer.getPitch() == 0)
			message += "0 (no bend)";
		else
			message += ("+" + midiPlayer.getPitch());
		Log.write(message);
		pitchSlider.setValue(midiPlayer.getPitch());
		pitchSpinner.setValue(midiPlayer.getPitch());
	}

	@Override
	public void onPositionChange(MidiPlayer midiPlayer) {
		Log.write("Playback position was set manually to "
				+ formatTime(midiPlayer.getPlayedMinutes(),
						midiPlayer.getPlayedSeconds()));
		updateTimeLabels();
	}

	@Override
	public void onResumePlaying(MidiPlayer midiPlayer) {
		Log.write("Playback resumed");
		setPlayPauseButton(false);
	}

	@Override
	public void onStartPlaying(MidiPlayer midiPlayer) {
		Log.write("Playback started");
		setPlayPauseButton(false);
	}

	@Override
	public void onStop(MidiPlayer midiPlayer) {
		Log.write("Playback stopped");
		positionSlider.setValue(0);
		updateTimeLabels();
		setPlayPauseButton(true);
	}

	@Override
	public void onTempoChange(MidiPlayer midiPlayer) {
		Log.write("Tempo changed to a factor of " + midiPlayer.getTempoFactor()
				+ "%");
		Log.write("Tempo now is " + midiPlayer.getTempoInBPM()
				+ " beats per minute");
		updateTimeLabels();
		totalLabel.setText(formatTime(midiPlayer.getTotalMinutes(),
				midiPlayer.getTotalSeconds()));
		tempoLabel.setText(String.valueOf(midiPlayer.getTempoInBPM()));
		positionSlider.setMaximum(midiPlayer.getDuration());
		positionSlider.setValue(midiPlayer.getPosition());
		tempoSpinner.setValue(midiPlayer.getTempoFactor());
		tempoSlider.setValue(midiPlayer.getTempoFactor());
	}

	@Override
	public void onVolumeChange(MidiPlayer midiPlayer) {
		Log.write("Global volume changed to " + midiPlayer.getVolume() + "%");
		setVolumeControls();
	}

	/* Track events */

	@Override
	public void onMuteStateChange(Track track) {
		if (track.isMute())
			Log.write(track + " has gone mute");
		else
			Log.write(track + " has gone unmute");
		setTrackVolumeControls(track);
	}

	@Override
	public void onSoloStateChange(Track track) {
		if (track.isSolo())
			Log.write(track + " is now soloing");
		else
			Log.write(track + " is not soloing anymore");
		setTrackSoloButton(track);
	}

	@Override
	public void onVolumeChange(Track track) {
		Log.write(track + " volume changed to " + track.getVolume() + "%");
		setTrackVolumeControls(track);
	}

	/* Updating the interface */

	protected void updateTimeLabels() {
		timeLabel.setText(formatTime(midiPlayer.getPlayedMinutes(),
				midiPlayer.getPlayedSeconds()));
		remainingLabel.setText(formatTime(midiPlayer.getMinutesLeft(),
				midiPlayer.getSecondsLeft()));
	}

	protected void setPlayPauseButton(boolean play) {
		ImageIcon icon;
		String toolTipText;
		if (play) {
			icon = Icon.get(Icon.PLAY);
			toolTipText = "Play";
		} else {
			icon = Icon.get(Icon.PAUSE);
			toolTipText = "Pause";
		}
		playPauseButton.setIcon(icon);
		playPauseButton.setToolTipText(toolTipText);
	}

	protected void setVolumeControls() {
		ImageIcon icon;
		String toolTipText;
		if (midiPlayer.isMute()) {
			icon = Icon.get(Icon.MUTED);
			toolTipText = "Unmute";
		} else {
			toolTipText = "Mute";
			if (midiPlayer.getVolume() >= 70)
				icon = Icon.get(Icon.VOLUME_HIGH);
			else if (midiPlayer.getVolume() >= 30)
				icon = Icon.get(Icon.VOLUME_MEDIUM);
			else
				icon = Icon.get(Icon.VOLUME_LOW);
		}
		volumeSlider.setValue(midiPlayer.getVolume());
		volumeSpinner.setValue(midiPlayer.getVolume());
		muteButton.setIcon(icon);
		muteButton.setToolTipText(toolTipText);
	}

	protected void setTrackSoloButton(Track track) {
		ImageIcon icon;
		String toolTipText;
		if (track.isSolo()) {
			icon = Icon.get(Icon.SOLO);
			toolTipText = "Remove " + track + " from solo";
		} else {
			icon = Icon.get(Icon.NO_SOLO);
			toolTipText = "Solo " + track;
		}
		JButton soloTrackButton = soloTrackButtons.get(track);
		soloTrackButton.setIcon(icon);
		soloTrackButton.setToolTipText(toolTipText);
	}

	protected void setTrackVolumeControls(Track track) {
		ImageIcon icon;
		String toolTipText;
		if (track.isMute()) {
			icon = Icon.get(Icon.MUTED);
			toolTipText = "Unmute " + track;
		} else {
			toolTipText = "Mute " + track;
			if (track.getVolume() >= 70)
				icon = Icon.get(Icon.VOLUME_HIGH);
			else if (track.getVolume() >= 30)
				icon = Icon.get(Icon.VOLUME_MEDIUM);
			else
				icon = Icon.get(Icon.VOLUME_LOW);
		}
		JButton muteTrackButton = muteTrackButtons.get(track);
		muteTrackButton.setIcon(icon);
		muteTrackButton.setToolTipText(toolTipText);
		trackVolumeSliders.get(track).setValue(track.getVolume());
		trackVolumeSpinners.get(track).setValue(track.getVolume());
	}

	/* Other methods */

	protected String addLeadingZero(long number) {
		if (number < 10)
			return "0" + number;
		else
			return String.valueOf(number);
	}

	protected String formatTime(int minutes, int seconds) {
		return addLeadingZero(minutes) + ":" + addLeadingZero(seconds);
	}

	protected JSpinner newPercentSpinner() {
		return new JSpinner(new SpinnerNumberModel(100, 0, 100, 1));
	}

	protected void pause() {
		timer.stop();
		midiPlayer.pause();
	}

	public void play() {
		midiPlayer.play();
		timer.start();
	}

	protected void stop() {
		timer.stop();
		midiPlayer.stop();
	}

}