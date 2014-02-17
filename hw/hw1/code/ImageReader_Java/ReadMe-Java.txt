You will find
	1) A program to display and manipulate images. This has been
given as java code to read an rgb image of the specified format explained in the homework and then displays it on screen
	2) Example images, which are in the native RGB formats. They are
as follows with dimensions and brief description
image1.rgb -> 640x480 shows a weather map
image2.rgb -> 640x480 shows an expanding spiral
image3.rgb -> 640x480 shows an image of text
image4.rgb -> 352x288 shows a construction site
image5.rgb -> 480x360 shows a city by night
image6.rgb -> 320x240 shows a few buildings with clouds
image7.rgb -> 512x512 shows an example of a gray (not color) image

Each image is different in the colors, frequency content etc. used and should
server as good examples for playing with subsampling, color space transformations,
quantization, compression etc.


Unzip the folder to where you want.
To run the code from command line, first compile with:

>> javac imageReader.java

and then, you can run to read a sample image (image1.rgb) as:

>> java imageReader image1.rgb 640 480
>> java imageReader image2.rgb 640 480
>> java imageReader image3.rgb 640 480
>> java imageReader image4.rgb 352 288
>> java imageReader image5.rgb 480 360
>> java imageReader image6.rgb 320 240
>> java imageReader image7.rgb 512 512

where, the first parameter is the image file name, second is the width and third is the height.