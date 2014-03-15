import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math;


public class imageReader {

    private static final int width = 352;
    private static final int height = 288;
    private static byte[] bytes = null;   //original data
    private static int[] buffer = new int[width * height * 3]; //compressed data
    private static byte[] decoded = new byte[width * height * 3]; //decoded data buffer
    private static int quantizationLevel = 0;
    private static int quantizationPower = 0;
    private static int displayMode = 1;
    private static int latency = 100;
    private static double cosine[][] = new double[8][8];

    private static void encode(){//encode using DCT
        int[] Rbuf = new int[64];
        int[] Gbuf = new int[64];
        int[] Bbuf = new int[64];


        for(int i = 0; i < 36; i++){
            for(int j = 0; j < 44; j++){
                for(int m = 0; m < 8; m++){
                    for(int n = 0; n < 8; n++){
                        int tmp = (int)bytes[i * 8 * width + j * 8 + m * width + n];
                        if(tmp >= 0)
                            Rbuf[m * 8 + n] = tmp;
                        else
                            Rbuf[m * 8 + n] = tmp + 256;
                        tmp = (int)bytes[i * 8 * width + j * 8 + m * width + n + width * height];
                        if(tmp >= 0)
                            Gbuf[m * 8 + n] = tmp;
                        else
                            Gbuf[m * 8 + n] = tmp + 256;
                        tmp = (int)bytes[i * 8 * width + j * 8 + m * width + n + width * height * 2];
                        if(tmp >= 0)
                            Bbuf[m * 8 + n] = tmp;
                        else
                            Bbuf[m * 8 + n] = tmp + 256;
                    }
                }
                int[] Rtemp = quantize(forwardDCT(Rbuf));
                int[] Gtemp = quantize(forwardDCT(Gbuf));
                int[] Btemp = quantize(forwardDCT(Bbuf));

                for(int x = 0; x < 8; x++){
                    for(int y = 0; y < 8; y++){
                        buffer[i * 8 * width + j * 8 + x * width + y] = Rtemp[x * 8 + y];
                        buffer[i * 8 * width + j * 8 + x * width + y + width * height] = Gtemp[x * 8 + y];
                        buffer[i * 8 * width + j * 8 + x * width + y + width * height * 2] = Btemp[x * 8 + y];
                    }
                }
            }
        }
    }

    private static void decode(){
        int[] Rbuf = new int[64];
        int[] Gbuf = new int[64];
        int[] Bbuf = new int[64];

        for(int i = 0; i < 36; i++){
            for(int j = 0; j < 44; j++){
                for(int m = 0; m < 8; m++){
                    for(int n = 0; n < 8; n++){
                        int tmp = buffer[i * 8 * width + j * 8 + m * width + n];
                        Rbuf[m * 8 + n] = tmp;

                        tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height];
                        Gbuf[m * 8 + n] = tmp;

                        tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height * 2];
                        Bbuf[m * 8 + n] = tmp;
                    }
                }
                int[] Rtemp = inverseDCT(dequantize(Rbuf));
                int[] Gtemp = inverseDCT(dequantize(Gbuf));
                int[] Btemp = inverseDCT(dequantize(Bbuf));


                for(int x = 0; x < 8; x++){
                    for(int y = 0; y < 8; y++){
                        decoded[i * 8 * width + j * 8 + x * width + y] = (byte)Rtemp[x * 8 + y];
                        decoded[i * 8 * width + j * 8 + x * width + y + width * height] = (byte)Gtemp[x * 8 + y];
                        decoded[i * 8 * width + j * 8 + x * width + y + width * height * 2] = (byte)Btemp[x * 8 + y];
                    }
                }
            }
        }
    }

    private static float[] forwardDCT(int[] pix){//single 8*8 block
        float[] ret = new float[64];
        for(int u = 0; u < 8; u++){
            for(int v = 0; v < 8; v++){
                double temp = 0.0f;
                for(int x = 0; x < 8; x++){
                    for (int y = 0; y < 8; y++){
                        temp += pix[x * 8 + y] * cosine[x][u] * cosine[y][v];
                    }
                }
                double m, n;
                if(u == 0)
                    m = 1 / Math.sqrt(2.0f);
                else
                    m = 1.0f;
                if(v == 0)
                    n = 1 / Math.sqrt(2.0f);
                else
                    n = 1.0f;
                ret[u * 8 + v] = (float)(1.0f / 4.0f * m * n * temp);
            }
        }
        return ret;
    }

    private static int inverseDCTSingle(int[] data, int x, int y){
        double temp = 0.0f;
        for(int u = 0; u < 8; u++){
            for (int v = 0; v < 8; v++){
                double m, n;
                if(u == 0)
                    m = 1 / Math.sqrt(2.0f);
                else
                    m = 1.0f;
                if(v == 0)
                    n = 1 / Math.sqrt(2.0f);
                else
                    n = 1.0f;
                temp += m * n * data[u * 8 + v] * cosine[x][u] * cosine[y][v];
            }
        }
        return (int)(temp * 1.0f /4.0f);
    }

    private static int[] inverseDCT(int[] data){
        int[] ret = new int[64];
        for(int x = 0; x < 8; x++){
            for(int y = 0; y < 8; y++){
                double temp = 0.0f;
                for(int u = 0; u < 8; u++){
                    for (int v = 0; v < 8; v++){
                        double m, n;
                        if(u == 0)
                            m = 1 / Math.sqrt(2.0f);
                        else
                            m = 1.0f;
                        if(v == 0)
                            n = 1 / Math.sqrt(2.0f);
                        else
                            n = 1.0f;
                        temp += m * n * data[u * 8 + v] * cosine[x][u] * cosine[y][v];
                    }
                }
                ret[x * 8 + y] = (int)(1.0f / 4.0f * temp);
            }
        }
        return ret;
    }

    private static int[] quantize(float[] data){
        int[] ret = new int[64];
        for(int i = 0; i < 64; i++){
            ret[i] = (int)(data[i] / quantizationPower);
        }
        return ret;
    }
    private static int[] dequantize(int[] data){
        int[] ret = new int[64];
        for(int i = 0; i < 64; i++){
            ret[i] = data[i] * quantizationPower;
        }
        return ret;
    }
    private static void readImage(String fileName){
        try {
            File file = new File(fileName);
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

    private static void baselineDecode(){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Use a label to display the image
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        int[] Rbuf = new int[64];
        int[] Gbuf = new int[64];
        int[] Bbuf = new int[64];

        for(int i = 0; i < 36; i++){
            for(int j = 0; j < 44; j++){
                for(int m = 0; m < 8; m++){
                    for(int n = 0; n < 8; n++){
                        int tmp = buffer[i * 8 * width + j * 8 + m * width + n];
                        Rbuf[m * 8 + n] = tmp;

                        tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height];
                        Gbuf[m * 8 + n] = tmp;

                        tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height * 2];
                        Bbuf[m * 8 + n] = tmp;
                    }
                }
                int[] Rtemp = inverseDCT(dequantize(Rbuf));
                int[] Gtemp = inverseDCT(dequantize(Gbuf));
                int[] Btemp = inverseDCT(dequantize(Bbuf));

                for(int x = 0; x < 8; x++){
                    for(int y = 0; y < 8; y++){
                        decoded[i * 8 * width + j * 8 + x * width + y] = (byte)Rtemp[x * 8 + y];
                        decoded[i * 8 * width + j * 8 + x * width + y + width * height] = (byte)Gtemp[x * 8 + y];
                        decoded[i * 8 * width + j * 8 + x * width + y + width * height * 2] = (byte)Btemp[x * 8 + y];
                    }
                }
                updateImage(img, decoded);
                label.setIcon(new ImageIcon(img));
                try{
                    Thread.sleep(latency);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static void calcCosine(){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                cosine[i][j] = Math.cos((2 * i + 1) * j * Math.PI / 16.0f);
            }
        }
    }

    private static void spectralSelectionDecode(){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Use a label to display the image
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        int[] Rbuf = new int[64];
        int[] Gbuf = new int[64];
        int[] Bbuf = new int[64];

        for(int i = 0; i < width * height * 3; i++){  //dequantize first
            buffer[i] = buffer[i] * quantizationPower;
        }

        for(int index = 0; index < 64; index++){
            for(int i = 0; i < 36; i++){
                for(int j = 0; j < 44; j++){
                    for(int m = 0; m < 8; m++){
                        for(int n = 0; n < 8; n++){
                            int tmp = buffer[i * 8 * width + j * 8 + m * width + n];
                            Rbuf[m * 8 + n] = tmp;

                            tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height];
                            Gbuf[m * 8 + n] = tmp;

                            tmp = buffer[i * 8 * width + j * 8 + m * width + n + width * height * 2];
                            Bbuf[m * 8 + n] = tmp;
                        }
                    }
                    int mod = index % 8;
                    int mul = (index - mod) / 8;
                    int Rvalue = inverseDCTSingle(Rbuf, mul, mod);
                    int Gvalue = inverseDCTSingle(Gbuf, mul, mod);
                    int Bvalue = inverseDCTSingle(Bbuf, mul, mod);
                    decoded[i * 8 * width + j * 8 + mul * width + mod] = (byte)Rvalue;
                    decoded[i * 8 * width + j * 8 + mul * width + mod + width * height] = (byte)Gvalue;
                    decoded[i * 8 * width + j * 8 + mul * width + mod + width * height * 2] = (byte)Bvalue;
                }
            }
            updateImage(img, decoded);
            label.setIcon(new ImageIcon(img));
            try{
                Thread.sleep(latency);
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }
    }

    private static void successiveBitDecode(){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Use a label to display the image
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        int[] temp = new int[width * height * 3];
        for(int i = 0; i < width * height * 3; i++){
            temp[i] = buffer[i];
        }
        int bitmask = 0x80000000;
        for(int j = 0; j < 32; j++){
            if(j != 0)
                bitmask += (int)Math.pow(2, 31 - j);
            for(int m = 0; m < width * height * 3; m++){
                buffer[m] = temp[m] & bitmask;
            }
            decode();
            updateImage(img, decoded);
            label.setIcon(new ImageIcon(img));
            try{
                Thread.sleep(latency);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private static void updateImage(BufferedImage img, byte[] data){
        int ind = 0;
        for(int y = 0; y < height; y++){

            for(int x = 0; x < width; x++){

                byte a = 0;
                byte r = data[ind];
                byte g = data[ind+height*width];
                byte b = data[ind+height*width*2];

                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                img.setRGB(x,y,pix);
                ind++;
            }
        }
    }

    private static void displayImage(byte[] image, int width, int height){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        updateImage(img, image);
        // Use a label to display the image
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        String fileName = args[0];
        quantizationLevel = Integer.parseInt(args[1]);
        quantizationPower = (int)Math.pow(2, quantizationLevel);
        displayMode = Integer.parseInt(args[2]);
        latency = Integer.parseInt(args[3]);
        calcCosine();
        readImage(fileName);
        displayImage(bytes, width, height);
        encode();
        if(displayMode == 1)
            baselineDecode();
        else if(displayMode == 2)
            spectralSelectionDecode();
        else if(displayMode == 3)
            successiveBitDecode();
    }
}