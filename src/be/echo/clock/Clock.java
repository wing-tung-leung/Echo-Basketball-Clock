package be.echo.clock;

import java.awt.*;
import java.awt.LayoutManager;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import static be.echo.clock.Log.log;

public class Clock extends JFrame implements ActionListener {

	private final static int MINUTES_IN_QUARTER = 10;

	private final static int DEFAULT_SHOT_SECONDS = 24;

	private int gameTime;

	private int shotTime;

	private SoundClip buzzer;

	private JLabel gameTimeField;

	private JLabel shotTimeField;

	private Toolkit toolkit;

	private ClockAdjuster clockAdjuster = new ClockAdjuster();

	private KeyListener keyListener = new KeyListener();

	static private Timer timer;

	private boolean shotClockEnabled = true;

	private Font normalFont;

	private Font largeFont;

	private GridLayout normalLayout = new GridLayout(2, 1);

	private GridLayout noShotClockLayout = new GridLayout(1, 1);

	public Clock(int minutes) throws Exception {
		super("Clock");

		toolkit = getToolkit();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.ORANGE);
		
		setLayout(normalLayout);

		gameTimeField = new JLabel();
		gameTimeField.setForeground(Color.BLACK);
		gameTimeField.setHorizontalAlignment(SwingConstants.CENTER);

		Font oldFont = gameTimeField.getFont();
		normalFont = oldFont.deriveFont(400f);
		largeFont = oldFont.deriveFont(540f);
		gameTimeField.setFont(normalFont);

		add(gameTimeField);

		shotTimeField = new JLabel(Integer.toString(shotTime));
		add(shotTimeField);
		shotTimeField.setFont(normalFont);
		shotTimeField.setForeground(Color.BLACK);
		shotTimeField.setHorizontalAlignment(SwingConstants.CENTER);

		buzzer = new Buzzer();
		gameTime = 60 * minutes;
		updateGameClock();
		resetShotClock();
		
		addKeyListener(keyListener);

		MouseListener mouseListener = new MouseListener();
		addMouseListener(mouseListener);
		
		pack();
		setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	private void beep(int count) {
		try {
			for (int i = 0 ; i < count ; ++i) {
				toolkit.beep();
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			log("interrupted sleep in beep sequence");
		}
	}

	private void toggleClock() {
		if (timer.isRunning()) {
			log("stop at " + getCurrentTimes());
			timer.stop();
			gameTimeField.setForeground(Color.BLACK);
			getContentPane().setBackground(Color.ORANGE);
		} else {
			log("start at " + getCurrentTimes());
			if (shotTime <= 0) {
				resetShotClock();
			}
			timer.start();
			gameTimeField.setForeground(Color.RED);
			getContentPane().setBackground(Color.WHITE);
		}
		beep(3);
	}
	
	private class KeyListener extends KeyAdapter {
		public void keyPressed(KeyEvent event) {
			switch (event.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					toggleClock();
					break;
				case KeyEvent.VK_ENTER:
					resetShotClock();
					break;
				case KeyEvent.VK_ESCAPE:
					log("buzz request: timer running = " + timer.isRunning());
					if (! timer.isRunning()) {
						SwingUtilities.invokeLater(new Buzzer());
					}
					break;
				case KeyEvent.VK_F5:
					log("xpert request: timer running = " + timer.isRunning());
					if (! timer.isRunning()) {
						log("enter xpert: time=" + getCurrentTimes());
						getContentPane().setBackground(Color.GRAY);
						removeKeyListener(this);
						addKeyListener(clockAdjuster);
					}
					break;
				case KeyEvent.VK_F9:
					log("request toggle shot clock");
					if (! timer.isRunning()) {
						toggleShotClock();
					}
					break;
			}
		}
	}

	private void toggleShotClock() {
		log("toggle shot clock");
		if (shotClockEnabled) {
			remove(shotTimeField);
			setLayout(noShotClockLayout);
			gameTimeField.setFont(largeFont);
		} else {
			gameTimeField.setFont(normalFont);
			setLayout(normalLayout);
			add(shotTimeField);
		}
		shotClockEnabled = ! shotClockEnabled;
		pack();
		setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	private class ClockAdjuster extends KeyAdapter {
		public void keyPressed(KeyEvent event) {
			switch (event.getKeyCode()) {
				case KeyEvent.VK_RIGHT:
					++gameTime;
					updateGameClock();
					break;
				case KeyEvent.VK_LEFT:
					if (gameTime > 0) {
						--gameTime;
						updateGameClock();
					}
					break;
				case KeyEvent.VK_UP:
					++shotTime;
					shotTimeField.setText(Integer.toString(shotTime));
					break;
				case KeyEvent.VK_DOWN:
					if (shotTime > 0) {
						--shotTime;
						shotTimeField.setText(Integer.toString(shotTime));
					}
					break;
				case KeyEvent.VK_F5:
					log("exit xpert: time=" + getCurrentTimes());
					removeKeyListener(this);
					addKeyListener(keyListener);
					getContentPane().setBackground(Color.ORANGE);
					break;
			}
		}
	}

	private String getCurrentTimes() {
		String gameTimeText = gameTimeField.getText();
		String shotTimeText = shotTimeField.getText();
		return "time=" + gameTimeText + ",shot=" + shotTimeText;
	}

	private class MouseListener extends MouseInputAdapter {
		public void mouseClicked(MouseEvent e) {
			switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					toggleClock();
					break;
				case MouseEvent.BUTTON3:
					resetShotClock();
					break;
			}
		}
	}

	private void timeOutShotClock() {
		if (shotClockEnabled) {
			log("shot time out at " + getCurrentTimes());
			buzzer.start();
			timer.stop();
			getContentPane().setBackground(Color.CYAN);
			buzzer = new Buzzer();
		}
	}

	private void warnTimeOutShot() {
		if (shotClockEnabled) {
			toolkit.beep();
		}
	}
	
	private class TimeUpdater implements Runnable {
		public void run() {
			--gameTime;
			if (gameTime > 0) {
				if (shotTime > 0) {
					--shotTime;
				}
				updateGameClock();
				shotTimeField.setText(Integer.toString(shotTime));
				if (shotTime <= 5 && shotTime > 0) {
					warnTimeOutShot();
				} else if (shotTime == 0) {
					timeOutShotClock();
				}
				if (gameTime == 60) {
					SoundClip cp = new SoundClip("dingdong.wav");
					cp.start();
				}
			} else {
				gameTimeField.setText("0:00");
				timer.stop();
				buzzer.start();
				buzzer = new Buzzer(); // maybe re-adding time manually
			}
		}
	}

	private void updateGameClock() {
		String minutes = Integer.toString(gameTime / 60);
		int seconds = gameTime % 60;
		String padding = seconds < 10 ? "0" : "";
		String secs = padding + seconds;
		gameTimeField.setText(minutes + ":" + secs);
	}

	private void resetShotClock() {
		log("reset shot " + getCurrentTimes());
		beep(1);
		shotTime = DEFAULT_SHOT_SECONDS;
		shotTimeField.setText(Integer.toString(shotTime));
	}

	public static void main(final String[] args) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					int minutes = MINUTES_IN_QUARTER;
					if (args.length == 1) {
						minutes = Integer.parseInt(args[0]);
					}
					Clock frame = new Clock(minutes);
					frame.setVisible(true);
					timer = new Timer(1000, frame);
					timer.setInitialDelay(500);
				
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void actionPerformed(ActionEvent event) {
		SwingUtilities.invokeLater(new TimeUpdater());
	}
}
