package be.echo.clock;

import java.awt.*;
import java.awt.LayoutManager;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.event.*;

public class Clock extends JFrame implements ActionListener {

	private final static int MINUTES_IN_QUARTER = 10;

	private final static int DEFAULT_SHOT_SECONDS = 24;

	private int gameTime;

	private int shotTime;

	private SoundClip buzzer;

	private JLabel playTimeField;

	private JLabel shotClockField;

	private Toolkit toolkit;

	private ClockAdjuster clockAdjuster = new ClockAdjuster();

	private KeyListener keyListener = new KeyListener();

	static private Timer timer;

	private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");

	private class Buzzer extends SoundClip {
		public Buzzer() {
			super("buzz.wav");
		}
	}

	private void log(String text) {
		System.out.print(timeFormat.format(new Date()));
		System.out.print(" : ");
		System.out.println(text);
	}

	public Clock(int minutes) throws Exception {
		super("Clock");

		toolkit = getToolkit();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.ORANGE);
		
		GridLayout manager = new GridLayout(2, 1);
		setLayout(manager);

		playTimeField = new JLabel();
		playTimeField.setForeground(Color.BLACK);
		playTimeField.setHorizontalAlignment(SwingConstants.CENTER);

		Font oldFont = playTimeField.getFont();
		Font f = oldFont.deriveFont(380f);
		playTimeField.setFont(f);

		add(playTimeField);

		shotClockField = new JLabel(Integer.toString(shotTime));
		add(shotClockField);
		shotClockField.setFont(f);
		shotClockField.setForeground(Color.BLACK);
		shotClockField.setHorizontalAlignment(SwingConstants.CENTER);

		buzzer = new Buzzer();
		gameTime = 60 * minutes;
		updateGameClock();
		resetShotClock();
		
		addKeyListener(keyListener);
		playTimeField.addKeyListener(keyListener);
		shotClockField.addKeyListener(keyListener);

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
			log("stop at " + gameTime);
			timer.stop();
			playTimeField.setForeground(Color.BLACK);
			getContentPane().setBackground(Color.ORANGE);
		} else {
			log("start at " + gameTime);
			if (shotTime <= 0) {
				resetShotClock();
			}
			timer.start();
			playTimeField.setForeground(Color.RED);
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
						getContentPane().setBackground(Color.GRAY);
						removeKeyListener(this);
						addKeyListener(clockAdjuster);
					}
					break;
			}
		}
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
					shotClockField.setText(Integer.toString(shotTime));
					break;
				case KeyEvent.VK_DOWN:
					if (shotTime > 0) {
						--shotTime;
						shotClockField.setText(Integer.toString(shotTime));
					}
					break;
				case KeyEvent.VK_F5:
					log("stopping xpert mode");
					removeKeyListener(this);
					addKeyListener(keyListener);
					getContentPane().setBackground(Color.ORANGE);
					break;
			}
		}
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
		buzzer.start();
		timer.stop();
		getContentPane().setBackground(Color.CYAN);
		buzzer = new Buzzer();
	}
	
	private class TimeUpdater implements Runnable {
		public void run() {
			--gameTime;
			if (gameTime > 0) {
				if (shotTime > 0) {
					--shotTime;
					if (shotTime <= 5 && shotTime > 0) {
						toolkit.beep();
					} else if (shotTime == 0) {
						timeOutShotClock();
					}
				}
				updateGameClock();
				shotClockField.setText(Integer.toString(shotTime));
				if (gameTime == 60) {
					SoundClip cp = new SoundClip("dingdong.wav");
					cp.start();
				}
			} else {
				playTimeField.setText("0:00");
				buzzer.start(); // no need for new buzzer, stopping anyway
				timer.stop();
			}
		}
	}

	private void updateGameClock() {
		String minutes = Integer.toString(gameTime / 60);
		int seconds = gameTime % 60;
		String padding = seconds < 10 ? "0" : "";
		String secs = padding + seconds;
		playTimeField.setText(minutes + ":" + secs);
	}

	private void resetShotClock() {
		log("resetShotClock()");
		beep(1);
		shotTime = DEFAULT_SHOT_SECONDS;
		shotClockField.setText(Integer.toString(shotTime));
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
//		this.repaint(100);
	}
}
