/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package hulo.localization.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

public class ImageHolder {

	static final int CDIM = 3;

	public static final Color RED = Color.red;
	public static final Color GREEN = Color.green;
	public static final Color BLUE = Color.blue;
	public static final Color WHITE = Color.white;
	public static final Color BLACK = Color.black;

	private BufferedImage image;
	private BufferedImage imageRaw;
	private Graphics2D g2;

	public ImageHolder(InputStream is){
		image = read(is);
		imageRaw = copyImage(image);
		g2 = image.createGraphics();
	}

	public ImageHolder(BufferedImage src){
		image = src;
		imageRaw = copyImage(image);
		g2 = image.createGraphics();
	}

	static BufferedImage copyImage(BufferedImage src){
		BufferedImage dist=new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
		dist.setData(src.getData());
		return dist;
	}

	public int rows(){
		return image.getHeight();
	}

	public int cols(){
		return image.getWidth();
	}

	public Color get(int x, int y){
		return getColor(image, x, y);
	}


	public void drawPoint(int x, int y, Color c){
		int width = 0;
		drawPoint(x, y, c, width);
	}

	public void drawPoint(int x, int y, Color c, int width){
		g2.setPaint(c);
		g2.draw(new Ellipse2D.Double(x,y, width, width));
	}

	public BufferedImage getImage(){
		return image;
	}

	public static BufferedImage read(InputStream is){
		BufferedImage read = null;
		try {
			read = ImageIO.read(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return read;
	}

	public static Color getColor(BufferedImage img, int x, int y){
		Color c = new Color(img.getRGB(x, y));
		return c;
	}

	public boolean isSameColor(Color c1, Color c2){
		return c1.equals(c2);
	}

	public void write(String ext, OutputStream ops){
		try {
			ImageIO.write(image, ext, ops);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ImageHolder revive(){
		return new ImageHolder(imageRaw);
	}

}
