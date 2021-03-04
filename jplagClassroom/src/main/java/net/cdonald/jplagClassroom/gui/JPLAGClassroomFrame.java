package net.cdonald.jplagClassroom.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.cdonald.jplagClassroom.mainProgramData.MainClassroomData;
import net.cdonald.jplagClassroom.utils.MyPreferences;

@SuppressWarnings("serial")
public class JPLAGClassroomFrame extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JPLAGClassroomFrame frame = new JPLAGClassroomFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public JPLAGClassroomFrame() {		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setTitle(MainClassroomData.APP_NAME);
		MyPreferences prefs = MyPreferences.getInstance();
		Dimension dimension = prefs.getDimension(MyPreferences.Dimensions.JPLAG, 800, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(dimension);
		contentPane = new MainPanel();
		setContentPane(contentPane);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				checkAndExit();
			}
		});

	}


	public void checkAndExit() {

		MyPreferences prefs = MyPreferences.getInstance();
		prefs.setDimension(MyPreferences.Dimensions.JPLAG, getSize());			
		dispose();
		System.gc();
	}
}



