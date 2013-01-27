package com.aleadam;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class FrapExperiment implements Serializable {
	private static final long serialVersionUID = -7364398383238755384L;
	public static final int FRAP_ROI = 0;
	public static final int STD_ROI = 1;
	public static final int BG_ROI = 2;

	private int WIDTH;
	private int HEIGHT;
	private ArrayList<BufferedImage> imageList;
	private ArrayList<Circle> frapRoiList, stdRoiList, bgRoiList;

	private String expName;
	
	private double[] timeStamps;

	public FrapExperiment (String name, ArrayList<BufferedImage> imageSequence, double[] time) {
		imageList = imageSequence;

		WIDTH = imageSequence.get(0).getWidth();
		HEIGHT = imageSequence.get(0).getHeight();

		frapRoiList = new ArrayList<Circle> ();
		stdRoiList = new ArrayList<Circle> ();
		bgRoiList = new ArrayList<Circle> ();

		expName = name;
		
		timeStamps = time;
	}

	public FrapExperiment (String name, BufferedImage[] imageSequence, double[] time) {
		imageList = new ArrayList<BufferedImage> ();
		for (int i=0; i<imageSequence.length; i++) {
			imageList.add(imageSequence[i]);
		}

		WIDTH = imageList.get(0).getWidth();
		HEIGHT = imageList.get(0).getHeight();

		frapRoiList = new ArrayList<Circle> ();
		stdRoiList = new ArrayList<Circle> ();
		bgRoiList = new ArrayList<Circle> ();

		expName = name;
		
		timeStamps = time;
	}
	
	public double[] getTimeStamps () { return timeStamps; }

	public void setName (String name) { expName = name; }

	public String getName () { return expName; }

	public boolean isRoiDefined (int roi, int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));
		ArrayList<Circle> ac = getCurrentRoiList (roi);
		return isRoiDefined (ac, frame);
	}

	public int getRoiSize (int roi) throws BadArgumentException {
		ArrayList<Circle> ac = getCurrentRoiList (roi);
		return ac.size();
	}

	public ArrayList<Circle> getRois (int roi) throws BadArgumentException {
		ArrayList<Circle> ac = getCurrentRoiList (roi);
		return ac;
	}

	public Circle getRoi (int roi, int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));

		return findInterpolatedRoi (roi, frame);
	}	

	public void addRoi (int roi, Circle c) throws BadArgumentException {
		if (c.f >= imageList.size() || c.f < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+c.f+")"));

		ArrayList<Circle> ac = getCurrentRoiList (roi);

		if (!isRoiDefined (ac, c.f)) {
			ac.add(c);
			sort (ac);
		}
		else {
			for (int i=0; i<ac.size(); i++) {
				int f = ac.get(i).f;
				if (c.f == f)
					ac.set(i, c);
			}
		}
	}

	public void setRois (int roi, ArrayList<Circle> roiList) throws BadArgumentException {
		ArrayList<Circle> ac = getCurrentRoiList (roi);
		ac.clear();
		for (Circle c : roiList) {
			addRoi (roi, c);
		}
	}

	public void remove (int roi, int index) throws BadArgumentException {
		ArrayList<Circle> ac = getCurrentRoiList (roi);
		if (index >= ac.size() || index < 0)
			throw (new BadArgumentException ("The requested ROI does not exist (index="+index+")"));

		ac.remove(index);
	}

	public double getData (int roi, int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));

		ArrayList<Circle> ac = getCurrentRoiList (roi);
		if (ac.size() == 0) 
			return -1;

		Circle c = findInterpolatedRoi (ac, frame);
		BufferedImage mask = new BufferedImage (4*WIDTH, 4*HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		mask.getGraphics().setColor(Color.WHITE);
		mask.getGraphics().fillOval(c.x - c.w, c.y - c.h, 2 * c.w, 2 * c.h);
		int mLeft = c.x - c.w;
		int mTop = c.y - c.h;
		int mRight = c.x + c.w;
		int mBottom = c.y + c.h;

		AffineTransform at = new AffineTransform ();
		at.scale(4, 4);
		AffineTransformOp scaleOp = new AffineTransformOp (at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		BufferedImage zoomedImage = new BufferedImage(4*WIDTH, 4*HEIGHT, BufferedImage.TYPE_USHORT_GRAY);
		zoomedImage = scaleOp.filter(imageList.get(frame), zoomedImage);

		double pixels = 0;
		int count = 0;
		Raster maskRaster = mask.getRaster();
		Raster imageRaster = zoomedImage.getRaster();
		for (int y=mTop; y<mBottom; y++) {
			for (int x=mLeft; x<mRight; x++) {
				int maskPx = maskRaster.getSample(x, y, 0);
				if (maskPx != 0) {
					int px  = imageRaster.getSample(x, y, 0);
					pixels += px;
					count++;
				}
			}
		}
		return pixels / count;
	}

	public double getTotalIntensity (int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));

		BufferedImage im = imageList.get(frame);
		double pixels = 0;
		int count = 0;
		Raster imageRaster = im.getRaster();
		for (int y=0; y<HEIGHT; y++) {
			for (int x=0; x<WIDTH; x++) {
				int px  = imageRaster.getSample(x, y, 0);
				pixels += px;
				count++;
			}
		}
		return pixels / count;
	}

	public BufferedImage getImage (int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));

		return imageList.get(frame);
	}

	public ArrayList<BufferedImage> getAllImages () {
		return imageList;
	}

	public int getWidth () { return WIDTH; }

	public int getHeight () { return HEIGHT; }

	public int getNFrames () { return imageList.size(); }

	private Circle findInterpolatedRoi (int roi, int frame) throws BadArgumentException {
		if (frame >= imageList.size() || frame < 0)
			throw (new BadArgumentException ("The requested frame is out of the image sequence range (frame="+frame+")"));

		ArrayList<Circle> ac = getCurrentRoiList (roi);
		if (ac.size() == 0) return null;

		Circle c1 = null, c2 = null;
		for (int i=0; i<frame; i++) {
			if (isRoiDefined (ac, i)) { c1 = getByFrame (ac, i); }
		}
		for (int i=imageList.size()-1; i>=frame; i--) {
			if (isRoiDefined (ac, i)) { c2 = getByFrame (ac, i); }
		}

		Circle c;
		if (c1 == null) { c = c2; }
		else if (c2 == null) { c = c1; }
		else { c = interpolate (frame, c1, c2); }

		return c;
	}


	private Circle findInterpolatedRoi (ArrayList<Circle> ac, int frame) {
		if (ac.size() == 0) return null;

		Circle c1 = null, c2 = null;
		for (int i=0; i<frame; i++) {
			if (isRoiDefined (ac, i)) { c1 = getByFrame (ac, i); }
		}
		for (int i=imageList.size()-1; i>=frame; i--) {
			if (isRoiDefined (ac, i)) { c2 = getByFrame (ac, i); }
		}

		Circle c;
		if (c1 == null) { c = c2; }
		else if (c2 == null) { c = c1; }
		else { c = interpolate (frame, c1, c2); }

		return c;
	}


	private boolean isRoiDefined (ArrayList<Circle> ac, int frame) {
		for (int i=0; i<ac.size(); i++) {
			int f = ac.get(i).f;
			if (f == frame) {
				return true;
			}
		}
		return false;
	}

	private Circle getByFrame (ArrayList<Circle> currentRoiListModel, int frame) {
		for (int i=0; i<currentRoiListModel.size(); i++) {
			Circle c = currentRoiListModel.get(i);
			if (c.f == frame) {
				return c;
			}
		}
		return null;
	}

	private Circle interpolate (int frame, Circle c1, Circle c2) {
		if (c1.f == c2.f) return c1;
		if (frame <= c1.f) return c1;
		if (frame >= c2.f) return c2; 

		if (c2.f < c1.f) {
			Circle c = c1;
			c1 = c2;
			c2 = c;
		}

		int x, y, w, h;
		x = c1.x + (c2.x - c1.x) * (frame - c1.f) / (c2.f - c1.f);
		y = c1.y + (c2.y - c1.y) * (frame - c1.f) / (c2.f - c1.f);
		w = c1.w + (c2.w - c1.w) * (frame - c1.f) / (c2.f - c1.f);
		h = c1.h + (c2.h - c1.h) * (frame - c1.f) / (c2.f - c1.f);

		return new Circle (frame, x, y, w, h);
	}

	private void sort(ArrayList<Circle> listModel) { 
		Circle[] circles = new Circle [listModel.size()];
		for (int i=0; i<circles.length; i++)
			circles[i] = listModel.get (i);
		Arrays.sort (circles);
		listModel.clear ();
		for (Circle c : circles)
			listModel.add (c);
	}

	private ArrayList<Circle> getCurrentRoiList (int roi) throws BadArgumentException {
		ArrayList<Circle> ac;
		switch (roi) {
		case FRAP_ROI:
			ac = frapRoiList;
			break;
		case STD_ROI:
			ac = stdRoiList;
			break;
		case BG_ROI:
			ac = bgRoiList;
			break;
		default:
			throw (new BadArgumentException ("Argument must be FRAP_ROI, STD_ROI or BG_ROI (arg="+roi+")"));
		}
		return ac;
	}

	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject (expName);
		String[] str = new String[3];
		str[0] = "";
		str[1] = "";
		str[2] = "";
		for (Circle c : frapRoiList)
			str[0] += c.toString() + ",";
		for (Circle c : stdRoiList)
			str[1] += c.toString() + ",";
		for (Circle c : bgRoiList)
			str[2] += c.toString() + ",";

		out.writeObject(str);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		expName=(String)in.readObject();

		Dimension d = (Dimension) in.readObject();
		WIDTH = d.width;
		HEIGHT = d.height;

		imageList = new ArrayList<BufferedImage> ();
		int size = in.readInt();
		for (int i=0; i<size; i++){
			float[] px = (float[]) in.readObject();
			BufferedImage bf = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_USHORT_GRAY);
			bf.getRaster().setPixels(0, 0, WIDTH, HEIGHT, px);
			imageList.add (bf);
		}
		
		frapRoiList = (ArrayList<Circle>) in.readObject();
		stdRoiList = (ArrayList<Circle>) in.readObject();
		bgRoiList = (ArrayList<Circle>) in.readObject();
	}

}
