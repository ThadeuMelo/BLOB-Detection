// Blob Finder Demo
// A.Greensted
// http://www.labbookpages.co.uk
// Please use however you like. I'd be happy to hear any feedback or comments.

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import java.awt.color.*;

public class RGBFrame extends JComponent
{
	private int width;
	private int height;
	private Dimension size;

	private byte[] data;

	private PixelInterleavedSampleModel sampleModel;
	private DataBufferByte dataBuffer;
	private WritableRaster raster;
	private ColorModel colourModel;
	private BufferedImage image;

	private Graphics2D graphics;

	private int[] colOrder = null;

	private String title;

	public RGBFrame(int width, int height, byte[] data)
	{
		this(width, height, data, null, new int[] {2,1,0});
	}

	public RGBFrame(int width, int height, byte[] data, String title)
	{
		this(width, height, data, title, new int[] {2,1,0});
	}

	public RGBFrame(int width, int height, byte[] data, int[] colOrder)
	{
		this(width, height, data, null, colOrder);
	}

	public RGBFrame(int width, int height, byte[] data, String title, int[] colOrder)
	{
		this.width = width;
		this.height = height;
		this.data = data;
		this.title = title;
		this.colOrder = colOrder;

		size = new Dimension(width, height);

		dataBuffer = new DataBufferByte(data, data.length);
		sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 3, 3*width, colOrder);

		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		colourModel = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

		raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0,0));

		image = new BufferedImage(colourModel, raster, false, null);
	}

	public Graphics2D getBufferImageGraphics()
	{
		if (graphics == null) graphics = image.createGraphics();
		return graphics;
	}

	public Dimension getSize()
	{
		return size;
	}

	public Dimension getPreferredSize()
	{
		return size;
	}

	public void paintComponent(Graphics g)
	{
		if (image != null) g.drawImage(image, 0, 0, this);

		if (title != null) {
			g.setColor(Color.RED);
			g.drawString(title, 5, height - 5);
		}
	}
}
