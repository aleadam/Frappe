package com.aleadam;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;

public class ExpFrame extends JPanel implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 4852332549648969908L;

	private FrapExperiment exp;

	private JLabel imageLabel;
	private JScrollBar scrollBar;
	private SpinnerNumberModel scaleModel, offsetModel, bleachModel;
	private JRadioButton frapRoiButton, stdRoiButton, bgRoiButton;
	private JList<Circle> roiList;
	private JButton addButton, deleteButton;
	private JLabel roiLabel;
	private SpinnerNumberModel xModel, yModel, widthModel, heightModel;

	private boolean roiStart, roiMove;
	private int roi;
	private int roiX, roiY;
	private Color colorRoi, colorInterpolatedRoi;

	private double zoomLevel = 1;

	public ExpFrame (FrapExperiment e) {
		try {
			exp = e;
			createWindow ();
			updateRoiList ();
			updateImage ();
		} catch (BadArgumentException e1) {
			e1.printStackTrace();
		}
	}	

	public ExpFrame(String name, ArrayList<BufferedImage> images, double[] time) {
		exp = new FrapExperiment (name, images, time);
		try {
			createWindow ();
		} catch (BadArgumentException e) {
			e.printStackTrace();
		}
	}

	public ExpFrame (File f) throws DependencyException, ServiceException, FormatException, IOException {
		if (f != null) {
			ArrayList<BufferedImage> images = loadFile (f);
			double[] time;
			time = loadTimeStamps (f);
			if (images == null) return;
			exp = new FrapExperiment (f.getPath(), images, time);
			try {
				createWindow ();
			} catch (BadArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	private ArrayList<BufferedImage> loadFile (File file) {
		try {
			ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
			ImagePlus imp = BF.openImagePlus(file.getPath())[0];
			int w = imp.getWidth();
			int h = imp.getHeight();
			ImageStack is = imp.getStack();
			int size = is.getSize();
			for (int i=0; i<size; i++) {
				ImageProcessor ip = is.getProcessor(i+1);
				float[] newData = new float [w*h];
				for (int y=0; y<h; y++) {
					for (int x=0; x<w; x++) {
						newData[y*w+x] = ip.getPixel(x, y);
					}
				}
				BufferedImage bf = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
				bf.getRaster().setPixels(0, 0, w, h, newData);
				images.add(bf);
			}
			return images;
		} catch (FormatException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private double[] loadTimeStamps (File file) throws DependencyException, ServiceException, FormatException, IOException {
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		IMetadata meta = service.createOMEXMLMetadata();

		IFormatReader reader = new ImageReader();
		reader.setMetadataStore(meta);
		reader.setId(file.getPath());
		int planeCount = meta.getPlaneCount(0);
		double[] time = new double[planeCount]; 
		for (int i = 0; i < planeCount; i++) {
			Double deltaT = meta.getPlaneDeltaT(0, i);
			if (deltaT == null) continue;
			time[i] = deltaT;
		}
		return time;
	}

	public FrapExperiment getFrapExp () { return exp; }

	public int getNFrames () { return exp.getNFrames(); }

	public String getExpName () { return exp.getName(); }

	public double[] getData() throws BadArgumentException {
		int size = exp.getNFrames();
		double[] data = new double [size];
		for (int i=0; i<size; i++) {
			data[i] = exp.getData (roi, i);
		}
		return data;
	}
	
	public String getCurrentRoi () {
		String roiName = "";
		switch (roi) {
		case 0:
			roiName = "FRAP ROI";
			break;
		case 1:
			roiName = "Standard ROI";
			break;
		case 2:
			roiName = "Background";
		}
		return roiName;
	}

	public int getBleachFrame () { return (Integer) bleachModel.getNumber() - 1; }

	public void setBleachFrame (int frame) { bleachModel.setValue(frame + 1); }

	public double[][] getAllData() throws BadArgumentException {
		int size = exp.getNFrames();
		double[][] data = new double [4][size];
		for (int i=0; i<size; i++) {
			data[0][i] = exp.getData (FrapExperiment.FRAP_ROI, i);
			data[1][i] = exp.getData (FrapExperiment.STD_ROI, i);
			data[2][i] = exp.getData (FrapExperiment.BG_ROI, i);
			data[3][i] = exp.getTotalIntensity (i);
		}
		return data;
	}

	public double[] doubleNormalization () throws BadArgumentException {
			int bleachFrame = getBleachFrame ();
			double[][] data = getAllData ();
			return doubleNormalization (data, bleachFrame);
	}

	private double[] doubleNormalization (double[][] data, int bleachFrame) {
		int size = data[0].length;
		double[] frap = data[0];
		double[] whole = data[3];
		double base[] = data[2];

		double whole_pre = 0;
		double frap_pre = 0;
		for (int i=0; i<bleachFrame; i++) {
			frap_pre += (frap[i]-base[i])/bleachFrame;
			whole_pre += (whole[i]-base[i])/bleachFrame;
		}


		double[] normalizedData = new double[size];
		for (int i=0; i<size; i++) {
			normalizedData[i] = ((frap[i]-base[i]) / (whole[i]-base[i])) * (whole_pre/frap_pre);
		}
		return normalizedData;
	}

	public double[] fullScale () throws BadArgumentException {
			int bleachFrame = getBleachFrame ();
			double[][] data = getAllData ();
			int size = data[0].length;
			double[] norm = doubleNormalization (data, bleachFrame);

			double frap_bleach = norm[bleachFrame];
			double frap_pre = 0;
			for (int i=0; i<bleachFrame; i++) {
				frap_pre += norm[i]/bleachFrame;
			}
			double[] normalizedData = new double[size];
			for (int i=0; i<size; i++) {
				normalizedData[i] = (norm[i]-frap_bleach) / (frap_pre-frap_bleach);
			}
			return normalizedData;
	}

	public String[] getRoiList () {
		try {
			String[] str = new String[3];
			str[0] = "";
			str[1] = "";
			str[2] = "";
			for (Circle c : exp.getRois(FrapExperiment.FRAP_ROI))
				str[0] += c.toString() + ",";
			for (Circle c : exp.getRois(FrapExperiment.STD_ROI))
				str[1] += c.toString() + ",";
			for (Circle c : exp.getRois(FrapExperiment.BG_ROI))
				str[2] += c.toString() + ",";

			return str;
		} catch (BadArgumentException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setRois (int roi, String roiList) throws BadArgumentException {
		ArrayList<Circle> roiArrayList = new ArrayList<Circle> ();
		for (String c : roiList.split(",")) {
			if (!c.equals("")) {
				int f = Integer.valueOf(c.split("-")[0])-1;
				int x = Integer.valueOf(c.split("-")[1]);
				int y = Integer.valueOf(c.split("-")[2]);
				int w = Integer.valueOf(c.split("-")[3]);
				int h = Integer.valueOf(c.split("-")[4]);
				roiArrayList.add(new Circle (f, x, y, w, h));
			}
		}

		exp.setRois (roi, roiArrayList);
		updateRoiList ();
		updateImage ();
	}

	private void createWindow () throws BadArgumentException {
		this.setLayout (new BorderLayout());

		int width = exp.getWidth();
		int height = exp.getHeight();

		colorRoi = Color.BLUE;
		colorInterpolatedRoi = new Color (100, 100, 255);

		BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
		image.getGraphics().drawImage(exp.getImage(0), 0, 0, null);

		imageLabel = new JLabel(new ImageIcon(image));
		imageLabel.addMouseListener(this);
		imageLabel.addMouseMotionListener(this);

		roiList = new JList<Circle>(new DefaultListModel<Circle>());
		roiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		roiList.setLayoutOrientation(JList.VERTICAL);

		xModel = new SpinnerNumberModel(1, 1, exp.getWidth(), 1);
		yModel = new SpinnerNumberModel(1, 1, exp.getHeight(), 1);
		widthModel = new SpinnerNumberModel(1, 1,  exp.getWidth(), 1);
		heightModel = new SpinnerNumberModel(1, 1, exp.getHeight(), 1);
		final JSpinner xSpinner = new JSpinner (xModel);
		final JSpinner ySpinner = new JSpinner (yModel);
		final JSpinner widthSpinner = new JSpinner (widthModel);
		final JSpinner heightSpinner = new JSpinner (heightModel);
		xModel.addChangeListener(new SpinnerChangeListener ());
		yModel.addChangeListener(new SpinnerChangeListener ());
		widthModel.addChangeListener(new SpinnerChangeListener ());
		heightModel.addChangeListener(new SpinnerChangeListener ());

		final JPanel topPanel = new JPanel ();
		topPanel.add(new JLabel ("Position: X"));
		topPanel.add(xSpinner);
		topPanel.add(new JLabel ("Y"));
		topPanel.add(ySpinner);
		topPanel.add(new JLabel ("Size: W"));
		topPanel.add(widthSpinner);
		topPanel.add(new JLabel ("Size: H"));
		topPanel.add(heightSpinner);

		addButton = new JButton ("Add ROI     ");
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				int n = scrollBar.getValue();
				int x = (int) ((int) xModel.getValue() * 4 / zoomLevel);
				int y = (int) ((int) yModel.getValue() * 4 / zoomLevel);
				int w = (int) ((int) widthModel.getValue() * 4 / zoomLevel);
				int h = (int) ((int) heightModel.getValue() * 4 / zoomLevel);

				try {
					exp.addRoi (roi, new Circle (n, x,y, w, h));
					updateRoiList ();
				} catch (BadArgumentException e) {
					e.printStackTrace();
				}
				int size = roiList.getModel().getSize();
				roiList.setSelectedIndex(size-1);
				roiList.ensureIndexIsVisible(size-1);
				addButton.setText("Replace ROI");
			}
		});
		deleteButton = new JButton ("Delete ROI");
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				int n = roiList.getSelectedIndex();
				if (n != -1) {
					try {
						exp.remove(roi, n);
						updateRoiList();
					} catch (BadArgumentException e) {
						e.printStackTrace();
					}
				}
			}
		});

		roiList.addListSelectionListener(new ListSelectionListener () {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() == false) {
					if (roiList.getSelectedIndex() == -1) {
						deleteButton.setEnabled(false);
					} else {
						deleteButton.setEnabled(true);
						Circle c = roiList.getSelectedValue();
						xModel.setValue((int) (c.x * zoomLevel/4));
						yModel.setValue((int) (c.y * zoomLevel/4));
						widthModel.setValue ((int) (c.w * zoomLevel/4));
						heightModel.setValue((int) (c.h * zoomLevel/4));
						scrollBar.setValue(c.f);
						try {
							updateImage();
						} catch (BadArgumentException e) {
							e.printStackTrace();
						}
					}
				}				
			}
		});

		topPanel.add(new JSeparator (JSeparator.VERTICAL));
		topPanel.add(addButton);
		topPanel.add(deleteButton);

		final JPanel rightPanel = new JPanel (new BorderLayout());
		roiLabel = new JLabel ("Current FRAP ROIs:");
		rightPanel.add(roiLabel, BorderLayout.NORTH);
		JScrollPane listScroller = new JScrollPane(roiList);
		rightPanel.add(listScroller, BorderLayout.CENTER);

		add (topPanel, BorderLayout.NORTH);
		add (rightPanel, BorderLayout.EAST);

		final JScrollPane imageScroller = new JScrollPane(imageLabel);
		imageScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		imageScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		add (imageScroller, BorderLayout.CENTER);

		int size = exp.getNFrames();
		final JLabel frameLabel = new JLabel ("1/" + size);

		scrollBar = new JScrollBar (JScrollBar.HORIZONTAL);
		scrollBar.setValues(0, 0, 1, size-1);
		scrollBar.setUnitIncrement(1);
		scrollBar.addAdjustmentListener(new AdjustmentListener () {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent event) {
				try {
					frameLabel.setText((event.getValue()+1)+"/"+exp.getNFrames());
					updateImage();
					int n = scrollBar.getValue();
					if (exp.isRoiDefined (roi, n))
						addButton.setText("Replace ROI");
					else
						addButton.setText("Add ROI    ");
				} catch (BadArgumentException e) {
					e.printStackTrace();
				}
			}
		});
		JPanel scrollBarPanel = new JPanel (new BorderLayout());
		scrollBarPanel.add (scrollBar, BorderLayout.CENTER);
		scrollBarPanel.add (frameLabel, BorderLayout.EAST);
		add (scrollBarPanel, BorderLayout.SOUTH);

		JPanel radioPanel = new JPanel (new BorderLayout());
		JPanel radioPanel2 = new JPanel ();
		radioPanel2.setLayout(new BoxLayout (radioPanel2, BoxLayout.Y_AXIS));
		radioPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		frapRoiButton = new JRadioButton ("Draw FRAP ROI");
		stdRoiButton = new JRadioButton ("Draw standard ROI");
		bgRoiButton = new JRadioButton ("Draw background ROI");
		ButtonGroup group = new ButtonGroup();
		group.add(frapRoiButton);
		group.add(stdRoiButton);
		group.add(bgRoiButton);
		frapRoiButton.setSelected(true);
		radioPanel2.add(frapRoiButton);
		radioPanel2.add(stdRoiButton);
		radioPanel2.add(bgRoiButton);
		radioPanel.add(radioPanel2, BorderLayout.CENTER);

		frapRoiButton.addActionListener(new RadioButtonActionListener ());
		stdRoiButton.addActionListener(new RadioButtonActionListener ());
		bgRoiButton.addActionListener(new RadioButtonActionListener ());

		JPanel contrastPanel = new JPanel ();
		contrastPanel.setLayout(new BoxLayout (contrastPanel, BoxLayout.Y_AXIS));
		contrastPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		contrastPanel.add(new JLabel ("Change contrast"));
		JPanel scalePanel = new JPanel ();
		scalePanel.setLayout(new BoxLayout (scalePanel, BoxLayout.X_AXIS));
		JPanel offsetPanel = new JPanel ();
		offsetPanel.setLayout(new BoxLayout (offsetPanel, BoxLayout.X_AXIS));
		scaleModel = new SpinnerNumberModel(1, 0.125, 16, 0.125);
		offsetModel = new SpinnerNumberModel(0, -0.5, 0.5, 0.01);
		final JSpinner scaleSpinner = new JSpinner (scaleModel);
		final JSpinner offsetSpinner = new JSpinner (offsetModel);
		scaleSpinner.addChangeListener(new SpinnerChangeListener ());
		offsetSpinner.addChangeListener(new SpinnerChangeListener ());
		scalePanel.add(new JLabel ("Scale   "));
		scalePanel.add(scaleSpinner);
		offsetPanel.add(new JLabel ("Offset   "));
		offsetPanel.add(offsetSpinner);
		contrastPanel.add(scalePanel);
		contrastPanel.add(offsetPanel);

		JPanel zoomPanel = new JPanel (new GridLayout(1, 3));
		zoomPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		final JLabel zoomLabel = new JLabel ("zoom: 1X");
		zoomLabel.setHorizontalTextPosition(JLabel.CENTER);
		JButton zoomOut = new JButton ("Zoom out");
		zoomOut.addActionListener(new ActionListener () {
			@Override public void actionPerformed(ActionEvent event) {
				try {
					if (zoomLevel > 0.125) {
						zoomLevel /= 2;
						zoomLabel.setText("zoom: " + zoomLevel + "X");
						xModel.setValue((int) xModel.getValue()/2);
						yModel.setValue((int) yModel.getValue()/2);
						widthModel.setValue ((int) Math.max((int) widthModel.getValue()/2, 1));
						heightModel.setValue((int) Math.max((int) heightModel.getValue()/2, 1));
						int x = imageScroller.getHorizontalScrollBar().getValue();
						int y = imageScroller.getVerticalScrollBar().getValue();
						updateImage ();
						imageScroller.getHorizontalScrollBar().setValue (x/2);
						imageScroller.getVerticalScrollBar().setValue (y/2);
					}
				} catch (BadArgumentException e) {
					e.printStackTrace();
				}
			}
		});
		JButton zoomIn = new JButton ("Zoom in");
		zoomIn.addActionListener(new ActionListener () {
			@Override public void actionPerformed(ActionEvent event) {
				try {
					if (zoomLevel < 16) {
						zoomLevel *= 2;
						zoomLabel.setText("zoom: " + zoomLevel + "X");
						xModel.setValue((int) xModel.getValue()*2);
						yModel.setValue((int) yModel.getValue()*2);
						widthModel.setValue ((int) widthModel.getValue()*2);
						heightModel.setValue((int) heightModel.getValue()*2);
						int x = imageScroller.getHorizontalScrollBar().getValue() + imageScroller.getHorizontalScrollBar().getVisibleAmount();
						int y = imageScroller.getVerticalScrollBar().getValue() + imageScroller.getVerticalScrollBar().getVisibleAmount();
						updateImage ();
						imageScroller.getHorizontalScrollBar().setValue (2*x);
						imageScroller.getVerticalScrollBar().setValue (2*y);
					}
				} catch (BadArgumentException e) {
					e.printStackTrace();
				}
			}
		});
		zoomPanel.add(zoomOut);
		zoomPanel.add(zoomLabel);
		zoomPanel.add(zoomIn);

		JPanel bleachPanel = new JPanel ();
		bleachPanel.setLayout(new BoxLayout (bleachPanel, BoxLayout.X_AXIS));
		bleachPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		bleachModel = new SpinnerNumberModel(1, 1, exp.getNFrames(), 1);
		final JSpinner bleachSpinner = new JSpinner (bleachModel);
		bleachPanel.add(new JLabel ("Bleach frame:   "));
		bleachPanel.add(bleachSpinner);

		JPanel leftPanel = new JPanel ();
		leftPanel.setLayout(new BoxLayout (leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(contrastPanel);
		leftPanel.add(zoomPanel);
		leftPanel.add(radioPanel);
		leftPanel.add(bleachPanel);
		JPanel spacerPanel1 = new JPanel(new BorderLayout());
		spacerPanel1.add(new JLabel (), BorderLayout.CENTER);
		leftPanel.add(spacerPanel1);
		JPanel spacerPanel2 = new JPanel(new BorderLayout());
		spacerPanel2.add(new JLabel (), BorderLayout.CENTER);
		leftPanel.add(spacerPanel2);
		add (leftPanel, BorderLayout.WEST);
	}

	private void updateImage () throws BadArgumentException {
		int width = (int) (exp.getWidth() * zoomLevel);
		int height = (int) (exp.getHeight() * zoomLevel);
		AffineTransform at = new AffineTransform ();
		at.scale(zoomLevel, zoomLevel);
		AffineTransformOp scaleOp = new AffineTransformOp (at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

		BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage zoomedImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
		zoomedImage = scaleOp.filter(exp.getImage(scrollBar.getValue()), zoomedImage);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.drawImage(zoomedImage, 0, 0, null);

		double scale = (double) scaleModel.getValue();
		double offset = (double) offsetModel.getValue() * 255;
		RescaleOp rescaleOp = new RescaleOp((float)scale, (float)offset, null);
		rescaleOp.filter(image, image);

		int x = (int) xModel.getValue();
		int y = (int) yModel.getValue();
		int w = (int) widthModel.getValue();
		int h = (int) heightModel.getValue();
		g.setColor(colorRoi);
		g.setStroke(new BasicStroke (4));
		g.drawOval(x - w, y - h, 2 * w, 2 * h);

		Circle c = exp.getRoi(roi, scrollBar.getValue());
		if (c != null) {
			g.setColor(colorInterpolatedRoi);
			g.setStroke(new BasicStroke (1));
			g.drawOval(c.x - c.w, c.y - c.h, 2 * c.w, 2 * c.h);
		}
		imageLabel.setIcon(new ImageIcon(image));
	}

	private void updateRoiList () throws BadArgumentException {
		DefaultListModel<Circle> dfl = (DefaultListModel<Circle>) roiList.getModel ();
		dfl.clear();
		for (Circle c : exp.getRois(roi)) {
			dfl.addElement(c);
		}
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		int mouseX;
		int mouseY;
		if (zoomLevel == 1) {
			mouseX = (int) (event.getX() - (imageLabel.getSize().width-exp.getWidth())/2);
			mouseY = (int) (event.getY() - (imageLabel.getSize().height-exp.getHeight())/2);
		} else if (zoomLevel >= 1) {
			mouseX = (int) (event.getX() - (imageLabel.getSize().width/zoomLevel-exp.getWidth()));
			mouseY = (int) (event.getY() - (imageLabel.getSize().height/zoomLevel-exp.getHeight()));
		} else {
			mouseX = (int) (event.getX() - Math.max ((imageLabel.getSize().width-exp.getWidth()*zoomLevel)/2, 0));
			mouseY = (int) (event.getY() - Math.max ((imageLabel.getSize().height-exp.getHeight()*zoomLevel)/2, 0));
		}

		roiList.clearSelection();
		int w = (int) widthModel.getValue();
		w = w == 0 ? 10 : w;
		int h = (int) heightModel.getValue();
		h = h == 0 ? 10 : h;
		xModel.setValue(mouseX);
		yModel.setValue(mouseY);
		widthModel.setValue (w);
		heightModel.setValue(h);
		try {
			updateImage ();
		} catch (BadArgumentException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void mousePressed(MouseEvent event) {
		int x = (int) xModel.getValue();
		int y = (int) yModel.getValue();
		int w = (int) widthModel.getValue();
		int h = (int) heightModel.getValue();

		if (zoomLevel == 1) {
			roiX = (int) (event.getX() - (imageLabel.getSize().width-exp.getWidth())/2);
			roiY = (int) (event.getY() - (imageLabel.getSize().height-exp.getHeight())/2);
		} else if (zoomLevel >= 1) {
			roiX = (int) (event.getX() - (imageLabel.getSize().width/zoomLevel-exp.getWidth()));
			roiY = (int) (event.getY() - (imageLabel.getSize().height/zoomLevel-exp.getHeight()));
		} else {
			roiX = (int) (event.getX() - Math.max ((imageLabel.getSize().width-exp.getWidth()*zoomLevel)/2, 0));
			roiY = (int) (event.getY() - Math.max ((imageLabel.getSize().height-exp.getHeight()*zoomLevel)/2, 0));
		}

		if (new Ellipse2D.Float (x-w, y-h, 2*w, 2*h).contains(roiX, roiY)) {
			roiMove = true;
			roiStart = false;
		} else {
			roiStart = true;
			roiMove = false;
			roiList.clearSelection();
		}
	}
	@Override
	public void mouseReleased(MouseEvent event) { 
		roiStart = false;
		roiMove = false;
	}
	@Override
	public void mouseDragged(MouseEvent event) {
		int mouseX;
		int mouseY;
		if (zoomLevel == 1) {
			mouseX = (int) (event.getX() - (imageLabel.getSize().width-exp.getWidth())/2);
			mouseY = (int) (event.getY() - (imageLabel.getSize().height-exp.getHeight())/2);
		} else if (zoomLevel >= 1) {
			mouseX = (int) (event.getX() - (imageLabel.getSize().width/zoomLevel-exp.getWidth()));
			mouseY = (int) (event.getY() - (imageLabel.getSize().height/zoomLevel-exp.getHeight()));
		} else {
			mouseX = (int) (event.getX() - Math.max ((imageLabel.getSize().width-exp.getWidth()*zoomLevel)/2, 0));
			mouseY = (int) (event.getY() - Math.max ((imageLabel.getSize().height-exp.getHeight()*zoomLevel)/2, 0));
		}

		if (roiStart) {
			xModel.setValue(roiX);
			yModel.setValue(roiY);
			widthModel.setValue (Math.abs(roiX - mouseX));
			heightModel.setValue(Math.abs(roiY - mouseY));
			try {
				updateImage ();
			} catch (BadArgumentException e) {
				e.printStackTrace();
			}
		} else if (roiMove) {
			int width = (int) (exp.getWidth()*zoomLevel);
			int height = (int) (exp.getHeight()*zoomLevel);
			int dx = mouseX - roiX;
			int dy = mouseY - roiY;
			int x = (int) xModel.getValue() + dx;
			x = x < 1 ? 1 : x;
			//			x = x > exp.getWidth() ? exp.getWidth() : x;
			x = x > width ? width : x;
			int y = (int) yModel.getValue() + dy;
			y = y < 1 ? 1 : y;
			y = y > height ? height : y;
			//			y = y > exp.getHeight() ? exp.getHeight() : y;
			xModel.setValue(x);
			yModel.setValue(y);
			roiX = mouseX;
			roiY = mouseY;
			try {
				updateImage ();
			} catch (BadArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	@Override public void mouseEntered(MouseEvent event) { }
	@Override public void mouseExited(MouseEvent event) { }
	@Override public void mouseMoved(MouseEvent event) { }

	private class SpinnerChangeListener implements ChangeListener {
		@Override public void stateChanged(ChangeEvent event) { 
			try {
				updateImage();
			} catch (BadArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	private class RadioButtonActionListener implements ActionListener {
		@Override public void actionPerformed(ActionEvent event) {
			if (frapRoiButton.isSelected()) {
				roi = FrapExperiment.FRAP_ROI;
				colorRoi = Color.BLUE;
				colorInterpolatedRoi = new Color (100, 100, 255);
				roiLabel.setText ("Current FRAP ROIs:      ");
			} else if (stdRoiButton.isSelected()) {
				roi = FrapExperiment.STD_ROI;
				colorRoi = Color.RED;
				colorInterpolatedRoi = new Color (255, 100, 100);
				roiLabel.setText ("Current Standard ROIs:  ");
			} else {
				roi = FrapExperiment.BG_ROI;
				colorRoi = Color.GREEN;
				colorInterpolatedRoi = new Color (100, 255, 100);
				roiLabel.setText ("Current Background ROIs:");
			}
			try {
				updateRoiList ();
				updateImage ();
				int n = scrollBar.getValue();

				if (exp.isRoiDefined (roi, n))
					addButton.setText("Replace ROI");
				else
					addButton.setText("Add ROI    ");
			} catch (BadArgumentException e) {
				e.printStackTrace();
			}
		}
	}
}
