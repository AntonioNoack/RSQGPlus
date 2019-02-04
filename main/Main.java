package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import main.gfx.StatusFrame;

public class Main {
	
	// 1.01
	public static int version = 101;
	
	public static State state = State.INIT;
	public static int position, maximal;
	public static enum State {
		
		INIT("Initializing"),
		LOADING_IMAGES("Loading Images #amount"),
		LOADING_POSTS("Loading Posts #amount"),
		READY("Ready");
		
		public String name;
		State(String name){
			this.name = name;
		}
		
	}
	
	public static Structure structure;
	public static StatusFrame statusFrame;
	public static int port = 8080;
	public static Server server;
	public static JFrame frame;
	
	public static final String title = "RSQGPlus by Antonio Noack";
	
	// request a repaint of the screen
	public static void repaint(){
		statusFrame.repaint();
	}

	public static void main(String[] args){
		
		askForUpdate();

		frame = new JFrame();
		frame.setTitle(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		statusFrame = new StatusFrame();
		frame.add(statusFrame);
		frame.setBounds(50, 50, 300, 120);
		frame.setVisible(true);
		
		structure = new Structure();
		
		server = new Server(structure, port);
		server.listenAlways();
		
	}
	
	public static void askForUpdate(){
		new Thread(new Runnable() {
			@Override public void run() {
				// TODO Auto-generated method stub
				try {
					URLConnection conurl = new URL("https://api.phychi.com/string/?app=RSQGPlus&id=0").openConnection();
					if(conurl instanceof HttpsURLConnection){
						HttpsURLConnection con = (HttpsURLConnection) conurl;
						con.setRequestProperty("User-Agent", "RSQGPlus V"+version);
						BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
						try {
							int newVersion = Integer.parseInt(reader.readLine());
							if(newVersion > version){
								Object[] options = {"OK"};
							    JOptionPane.showOptionDialog(null,
							    		"Please download the newest version at https://github.com/AntonioNoack/RSQGPlus! :)","New Update!",
							    		JOptionPane.PLAIN_MESSAGE, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
							}
						} catch(NumberFormatException e){
							// :(
						}
						reader.close();
					}
				} catch(IOException e){}
			}
		}).start();
	}
	
	public static int askNumber(String title, int defaultValue){
		try {
			String s = (String) JOptionPane.showInputDialog(null, null, title, JOptionPane.PLAIN_MESSAGE);
			if(s==null) return defaultValue;
			return Integer.parseInt(s);
		} catch(NumberFormatException e){}
		return defaultValue;
	}
	
}
