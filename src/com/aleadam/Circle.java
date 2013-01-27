package com.aleadam;

import java.io.Serializable;

public class Circle implements Comparable<Circle>, Serializable {
	private static final long serialVersionUID = -85595196946227099L;
	public int f, x, y, w, h;
	
	public Circle (int frame, int posX, int posY, int width, int height) {
		f = frame;
		x = posX;
		y = posY;
		w = width;
		h = height;
	}
	
	@Override
	public String toString () {
		return new String ((f+1) + "-" + x + "-" + y + "-" + w + "-" + h);
	}

	@Override
	public int compareTo(Circle c) {
        if (f < c.f) return -1;
        else if (f > c.f) return +1;
        return 0;
	}
}
