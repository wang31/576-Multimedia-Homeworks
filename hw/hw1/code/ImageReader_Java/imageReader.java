/*
* Zheng Wang's cs576 hw1
*
* */
import java.awt.*;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;
import javax.swing.ImageIcon;
import java.lang.*;
import java.lang.Integer;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.System;
import java.lang.Thread;

public class imageReader {
	private static final int width = 512, height = 512;
    private static byte[] bytes = null;
    private static BufferedImage[] animationFrames = null;
	//only for 2 by 2 matrix
	private static void inverseMatrix(float[][] m){
		float a = m[0][0];
		float b = m[0][1];
		float c = m[1][0];
		float d = m[1][1];
		float denominator = 1 / (a * d - b * c);
		m[0][0] = d * denominator;
		m[0][1] = -b * denominator;
		m[1][0] = -c * denominator;
		m[1][1] = a * denominator;
	}

    private static float[] inverseTransform(int[] coordinate, float scale, float angle){
        int x_new = coordinate[0];
        int y_new = coordinate[1];
        int x_transformed = x_new - width / 2;
        int y_transformed = y_new - height / 2;
        float[][] m = new float[2][2];//this stores the product of the scale matrix and rotation matrix
        float angle_in_radian = angle / 180.0f * (float)Math.PI;
        m[0][0] = scale * (float)Math.cos(angle_in_radian);
        m[0][1] = -scale * (float)Math.sin(angle_in_radian);
        m[1][0] = - m[0][1];
        m[1][1] = m[0][0];
        inverseMatrix(m);
        float x_origin = m[0][0] * x_transformed + m[0][1] * y_transformed;
        float y_origin = m[1][0] * x_transformed + m[1][1] * y_transformed;
        coordinate[0] = (int)Math.floor(x_origin + width / 2);
        coordinate[1] = (int)Math.floor(y_origin + height / 2);
        return new float[]{x_origin + width / 2, y_origin + height / 2};
    }


   private static void displayImage(BufferedImage image){
       if(image != null){
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(image));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
       }
   }

   private static void readFromFile(String filename){
       try {
            File file = new File(filename);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
               offset += numRead;
            }
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   private static void displayOriginalImage(){//to display original image
       if(bytes != null){
           BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
           //for the original image
           int ind = 0;
           for(int y = 0; y < height; y++){

               for(int x = 0; x < width; x++){

                   byte a = 0;
                   byte r = bytes[ind];
                   byte g = bytes[ind+height*width];
                   byte b = bytes[ind+height*width*2];

                   int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                   //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                   img.setRGB(x,y,pix);
                   ind++;
               }
           }
           displayImage(img);
        }
   }

   private static void constructAnimationFrames(float scale, float rotation, int antialiasing, int framerate, int time){
       if(bytes != null){
           int numberOfFrames = framerate * time;
           animationFrames = new BufferedImage[numberOfFrames];
           if(scale >= 1.0f){
                for(int i = 0; i < numberOfFrames; i++){
                   animationFrames[i] = constructModifiedImage(((scale - 1.0f) / numberOfFrames * i) + 1.0f, rotation / numberOfFrames * i, antialiasing);
                }
           }
           else if(scale > 0.0f){
               for(int i = 0; i < numberOfFrames; i++){
                   animationFrames[i] = constructModifiedImage(1.0f - ((1.0f - scale) / numberOfFrames * i), rotation / numberOfFrames * i, antialiasing);
               }
           }
       }
   }


   private static void playAnimation(int framerate){
       float interval = 1.0f / framerate;
       interval *= 1000000000;
       JFrame frame = new JFrame();
       JLabel label = new JLabel(new ImageIcon(animationFrames[0]));
       frame.getContentPane().add(label, BorderLayout.CENTER);
       frame.pack();
       frame.setVisible(true);
       long time = System.nanoTime();
       int i = 1;
       while(i < animationFrames.length){
           if(System.nanoTime() - time > interval){
               label.setIcon(new ImageIcon(animationFrames[i]));
                i++;
                time = System.nanoTime();
           }
       }
   }
   //display the original and the modified image
   private static BufferedImage constructModifiedImage(float scale, float rotation, int antialiasing){

       if(bytes != null && scale > 0.0f){
           BufferedImage img_modified = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

       int ind = 0;
       //for modified image
       for(int y = 0; y < height; y++){

            for(int x = 0; x < width; x++){
                int[] coordinate = new int[2];
                coordinate[0] = x;
                coordinate[1] = y;
                float[] coord_float = inverseTransform(coordinate, scale, rotation);
                if(coordinate[0] >= 0 && coordinate[1] >= 0 && coordinate[0] < width - 1 && coordinate[1] < height - 1){
                    if(antialiasing == 0){
                        ind = coordinate[0] + coordinate[1] * width;
                        byte a = 0;
                        byte r = bytes[ind];
                        byte g = bytes[ind+height*width];
                        byte b = bytes[ind+height*width*2];

                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                        img_modified.setRGB(x,y,pix);
                    }
                    else if(antialiasing == 1){//does antialiasing using bilinear interpolation


                        int ind_topleft = coordinate[0] + coordinate[1] * width;
                        int ind_topright = ind_topleft + 1;
                        int ind_bottomleft = ind_topleft + width;
                        int ind_bottomright = ind_bottomleft + 1;


                        //interpolation of the r value
                        Byte b1 = bytes[ind_topleft];
                           Byte b2 = bytes[ind_topright];
                           int b1_int;
                           int b2_int;
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;//convert to unsigned value
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float r_top = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;

                           b1 = bytes[ind_bottomleft];
                           b2 = bytes[ind_bottomright];
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float r_bottom = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;
                           float r_combined = ((float)coordinate[1] + 1.0f - coord_float[1]) * r_top + (coord_float[1] - (float)coordinate[1]) * r_bottom;

                           //interpolation of the g value
                           b1 = bytes[ind_topleft + height * width];
                           b2 = bytes[ind_topright + height * width];
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float g_top = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;

                           b1 = bytes[ind_bottomleft + height * width];
                           b2 = bytes[ind_bottomright + height * width];
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float g_bottom = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;
                           float g_combined = ((float)coordinate[1] + 1.0f - coord_float[1]) * g_top + (coord_float[1] - (float)coordinate[1]) * g_bottom;

                           //interpolation of the b value
                           b1 = bytes[ind_topleft + height * width * 2];
                           b2 = bytes[ind_topright + height * width * 2];
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float b_top = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;

                           b1 = bytes[ind_bottomleft + height * width * 2];
                           b2 = bytes[ind_bottomright + height * width * 2];
                           if(b1.intValue() < 0){
                               b1_int = b1.intValue() + 256;
                           }
                           else{
                               b1_int = b1.intValue();
                           }
                           if(b2.intValue() < 0){
                               b2_int = b2.intValue() + 256;
                           }
                           else{
                               b2_int = b2.intValue();
                           }
                           float b_bottom = ((float)coordinate[0] + 1.0f - coord_float[0]) * (float)b1_int + (coord_float[0] - (float)coordinate[0]) * (float)b2_int;
                           float b_combined = ((float)coordinate[1] + 1.0f - coord_float[1]) * b_top + (coord_float[1] - (float)coordinate[1]) * b_bottom;

                           byte a = 0;
                           int r_int = (int)Math.floor(r_combined);
                           int g_int = (int)Math.floor(g_combined);
                           int b_int = (int)Math.floor(b_combined);

                           byte r = (byte)r_int;
                           byte g = (byte)g_int;
                           byte b = (byte)b_int;


                           int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                           //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                           img_modified.setRGB(x,y,pix);
                       }
                   }
                   else{
                       byte a = 0;
                       byte r = (byte)255;
                       byte g = (byte)255;
                       byte b = (byte)255;

                       int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                       //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                       img_modified.setRGB(x,y,pix);
                   }
               }
           }
           return img_modified;
       }
       else return null;

   }


    public static void main(String[] args) {
	    String fileName = args[0];
        readFromFile(fileName);
	    float scale = Float.parseFloat(args[1]);
        float rotation = Float.parseFloat(args[2]);
        int antialiasing = Integer.parseInt(args[3]);
        if(args.length == 6){
            int framerate = Integer.parseInt(args[4]);
            int seconds = Integer.parseInt(args[5]);
            constructAnimationFrames(scale, rotation, antialiasing, framerate, seconds);
            playAnimation(framerate);
        }
        else{
            displayOriginalImage();
            displayImage(constructModifiedImage(scale, rotation, antialiasing));
        }
   }
  
}