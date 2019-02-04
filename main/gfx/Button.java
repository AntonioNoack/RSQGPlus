package main.gfx;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Button {

	public static void draw(Graphics g, FontMetrics fontMetrics, String text, Color color, float x, float y, float w, float h, int mx, int my){
		
		int ix = (int) x;
		int iw = (int) w;
		int iy = (int) y;
		int ih = (int) h;
		
		boolean isBeingHovered = mx > ix && my > iy && mx < ix + iw && my < iy + ih;
		
		if(color != null){
			g.setColor(isBeingHovered ? color.darker() : color);
			g.fillRect(ix, iy, iw, ih);
			
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(ix, iy, iw, ih);
			
			g.setColor(Color.BLACK);
		} else {// für das Histogram
			g.setColor(Color.WHITE);
		}
		
	    g.drawString(text,
	    		ix + (iw - fontMetrics.stringWidth(text))/2,
	    		iy + (ih - fontMetrics.getHeight())/2 + fontMetrics.getAscent());
		
	}
	

	public static void drawText(Graphics g, FontMetrics fontMetrics, String text, Color color, float x, float y, float w, float h, float x0, float dx){
		
		int ix = (int)(x0 + x * dx);
		int iw = (int)(w * dx);
		int iy = (int)(y * dx);
		int ih = (int)(h * dx);
		
		g.setColor(color);
	    g.drawString(text,
	    		ix + (iw - fontMetrics.stringWidth(text))/2,
	    		iy + (ih - fontMetrics.getHeight())/2 + fontMetrics.getAscent());
		
	}
	
	public static void draw(Graphics g, FontMetrics fontMetrics, String text, Color colorA, Color colorB, float x, float y, float w, float h, float x0, float dx, int mx, int my, boolean active){
		
		int ix = (int)(x0 + x * dx);
		int iw = (int)(w * dx);
		int iy = (int)(y * dx);
		int ih = (int)(h * dx);
		
		boolean isBeingHovered = mx > ix && my > iy && mx < ix + iw && my < iy + ih;
		
		g.setColor(active ? colorB : isBeingHovered ? colorA.darker() : colorA);
		g.fillRect(ix, iy, iw, ih);
		
		g.setColor(Color.LIGHT_GRAY);
		g.drawRect(ix, iy, iw, ih);
		
		g.setColor(Color.BLACK);
	    g.drawString(text,
	    		ix + (iw - fontMetrics.stringWidth(text))/2,
	    		iy + (ih - fontMetrics.getHeight())/2 + fontMetrics.getAscent());
		
	}
}
