// Blob Finder Demo
// A.Greensted
// http://www.labbookpages.co.uk
// Please use however you like. I'd be happy to hear any feedback or comments.

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class BlobDemo
{
	public BlobDemo(String filename)
	{
		// Load Source image
		BufferedImage srcImage = null;

		try
		{
			File imgFile = new File(filename);
			srcImage = javax.imageio.ImageIO.read(imgFile);
		}
		catch (IOException ioE)
		{
			System.err.println(ioE);
			System.exit(1);
		}

		int width = srcImage.getWidth();
		int height = srcImage.getHeight();

		// Get raw image data
		Raster raster = srcImage.getData();
		DataBuffer buffer = raster.getDataBuffer();

		int type = buffer.getDataType();
		if (type != DataBuffer.TYPE_BYTE)
		{
			System.err.println("Wrong image data type");
			System.exit(1);
		}
		if (buffer.getNumBanks() != 1)
		{
			System.err.println("Wrong image data format");
			System.exit(1);
		}

		DataBufferByte byteBuffer = (DataBufferByte) buffer;
		byte[] srcData = byteBuffer.getData(0);

		// Sanity check image
		if (width * height * 3 != srcData.length) {
			System.err.println("Unexpected image data size. Should be RGB image");
			System.exit(1);
		}

		// Output Image info
		System.out.printf("Loaded image: '%s', width: %d, height: %d, num bytes: %d\n", filename, width, height, srcData.length);

		// Create Monochrome version - using basic threshold technique
		byte[] monoData = new byte[width * height];
		int srcPtr = 0;
		int monoPtr = 0;

		while (srcPtr < srcData.length)
		{
			int val = ((srcData[srcPtr]&0xFF) + (srcData[srcPtr+1]&0xFF) + (srcData[srcPtr+2]&0xFF)) / 3;
			monoData[monoPtr] = (val > 128) ? (byte) 0xFF : 0;

			srcPtr += 3;
			monoPtr += 1;
		}

		byte[] dstData = new byte[srcData.length];

		// Create Blob Finder
		BlobFinder finder = new BlobFinder(width, height);

		ArrayList<BlobFinder.Blob> blobList = new ArrayList<BlobFinder.Blob>();
		finder.detectBlobs(monoData, dstData, 0, -1, (byte)0, blobList);

		// List Blobs
		System.out.printf("Found %d blobs:\n", blobList.size());
		for (BlobFinder.Blob blob : blobList) System.out.println(blob);

		// Create GUI
		RGBFrame srcFrame = new RGBFrame(width, height, srcData);
		RGBFrame dstFrame = new RGBFrame(width, height, dstData);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
		panel.add(srcFrame, BorderLayout.WEST);
		panel.add(dstFrame, BorderLayout.EAST);
		panel.add(new JLabel("A.Greensted - http://www.labbookpages.co.uk", JLabel.CENTER), BorderLayout.SOUTH);

		JFrame frame = new JFrame("Blob Detection Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args)
	{
		System.out.println("Blob Finder Demo - A.Greensted - http://www.labbookpages.co.uk");
		if (args.length<1) {
			System.err.println("Provide image filename");
			System.exit(1);
		}

		new BlobDemo(args[0]);
	}
}
