package pl.shockah.shocky;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class TimedActions implements ActionListener {
	private Timer timer;
	
	public TimedActions() {
		timer = new Timer(5*60*1000,this);
		timer.start();
	}
	
	public void actionPerformed(ActionEvent event) {
		timer.restart();
		Data.save();
	}
}