package main.gfx;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;

import main.Main;

public class StatusFrame extends JLabel {
	
	private static final long serialVersionUID = 1L;
	
	// Status: Ready
	// URL: localhost:8080
	// STOP
	
	public StatusFrame(){
		// Mouselistener für die Click-Events
		addMouseListener(new MouseListener() {
			
			@Override public void mouseReleased(MouseEvent arg0) {}
			@Override public void mousePressed(MouseEvent arg0) {}
			@Override public void mouseExited(MouseEvent arg0) {}
			@Override public void mouseEntered(MouseEvent arg0) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				int y = e.getY();
				float fy = 1f * y / height;
				if(fy > .333f){
					if(fy > .667f){
						// stop
						Main.server.close();
						System.exit(0);
					} else {
						// ask new port, ggf save to config
						int newPort = Main.askNumber("New port? Default: 8080", Main.port);
						if(newPort > -1 && newPort < 65535 && newPort != Main.port){
							Main.port = newPort;
							Main.server.restart(newPort);
							Main.structure.writeConfig();
							repaint();
						}
					}
				}
			}
		});
		
		// MouseMotion für die Hovering-Changing-Events,
		// auch wenn ich sie nur direkt und ineffizient abfange
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override public void mouseMoved(MouseEvent e) {
				mx = e.getX();
				my = e.getY();
				repaint();
			}
			
			@Override public void mouseDragged(MouseEvent arg0) {}
		});
	}
	
	private int mx, my;
	private FontMetrics menueFontMetrics;
	
	private Color
	
			pColor = c(0x4ba5ff),
			nnColor = c(0x8ec600),
			nColor = c(0xc4ff2d);
	
	private float width, height;
	
	// die Haupt-Mal-Funktion
	public void paint(Graphics g) {
		
		int iw = getWidth();
		int ih = getHeight();
		
		width = iw;
		height = ih;
		
		float fontHeight = height * .3f * .5f;

		Font menueFont = getFont().deriveFont(fontHeight);
		menueFontMetrics = g.getFontMetrics(menueFont);
		
		g.setFont(menueFont);
		
		Button.draw(g, menueFontMetrics, "Status: "+Main.state.name.replace("#amount", Main.position+"/"+Main.maximal),
				nColor, 0f, 0f, width, height/3, -1, -1);
		Button.draw(g, menueFontMetrics, "URL: http://localhost:"+Main.port,
				nnColor, 0f, height/3, width, height/3, mx, my);
		Button.draw(g, menueFontMetrics, "STOP Server",
				pColor, 0f, 2*height/3, width, height/3, mx, my);
		
	}
	
	// https://stackoverflow.com/questions/7896280/converting-from-hsv-hsb-in-java-to-rgb-without-using-java-awt-color-disallowe
	public static Color fromHSV(float hue, float saturation, float value) {
	
	    int h = (int)(hue * 6);
	    float f = hue * 6 - h;
	    float p = value * (1 - saturation);
	    float q = value * (1 - f * saturation);
	    float t = value * (1 - (1 - f) * saturation);

	    switch (h % 6) {
		    case 0: return new Color(value, t, p);
		    case 1: return new Color(q, value, p);
		    case 2: return new Color(p, value, t);
		    case 3: return new Color(p, q, value);
		    case 4: return new Color(t, p, value);
		    default: return new Color(value, p, q);
	    }
	}
	
	// Farbe from Hex
	public static Color c(int hex){
		return new Color((hex >> 16) & 255 , (hex >> 8) & 255, hex & 255);
	}
	
	// Farbe from Hex und alpha
	public static Color c(int hex, int alpha){
		return new Color((hex >> 16) & 255 , (hex >> 8) & 255, hex & 255, alpha);
	}
}
