/*
	
*/

#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <map>
#include <ctime>

#include "cv.h"
#include "highgui.h"

using namespace std;

struct coordinate
{
	unsigned int x, y;
	void * data;
};

struct lineBlob
{
	unsigned int min, max;
	unsigned int blobId;
	bool attached;
};

struct blob
{
	//unsigned int blobId;
	coordinate min, max;
	coordinate center;
};

bool detectBlobs(IplImage* frame, IplImage* finalFrame)
{
	int blobCounter = 0;
	map<unsigned int, blob> blobs;

    unsigned char threshold = 235;


    vector< vector<lineBlob> > imgData(frame->width);
	for(int row = 0; row < frame->height; ++row)
	{
		for(int column = 0; column < frame->width; ++column)
		{
			//unsigned char byte = (unsigned char) imgStream.get();
			unsigned char byte = (unsigned char) frame->imageData[(row*frame->width)+ column];
			if(byte >= threshold)
			{
				int start = column;
				for(;byte >= threshold; byte = (unsigned char) frame->imageData[(row*frame->width)+ column], ++column);
				int stop = column-1;
				lineBlob lineBlobData = {start, stop, blobCounter, false};
				imgData[row].push_back(lineBlobData);
				blobCounter++;
			}
		}
	}

	/* Check lineBlobs for a touching lineblob on the next row */
	for(int row = 0; row < imgData.size(); ++row)
	{
		for(int entryLine1 = 0; entryLine1 < imgData[row].size(); ++entryLine1)
		{
			for(int entryLine2 = 0; entryLine2 < imgData[row+1].size(); ++entryLine2)
			{
				if(!((imgData[row][entryLine1].max < imgData[row+1][entryLine2].min) || (imgData[row][entryLine1].min > imgData[row+1][entryLine2].max)))
				{
					if(imgData[row+1][entryLine2].attached == false)
					{
						imgData[row+1][entryLine2].blobId = imgData[row][entryLine1].blobId;
						imgData[row+1][entryLine2].attached = true;
					}
					else
					{
						imgData[row][entryLine1].blobId = imgData[row+1][entryLine2].blobId;
						imgData[row][entryLine1].attached = true;
					}
				}
			}
		}
	}

	// Sort and group blobs
	for(int row = 0; row < imgData.size(); ++row)
	{
		for(int entry = 0; entry < imgData[row].size(); ++entry)
		{
			if(blobs.find(imgData[row][entry].blobId) == blobs.end()) // Blob does not exist yet
			{
				blob blobData = {{imgData[row][entry].min, row}, {imgData[row][entry].max, row}, {0,0}};
				blobs[imgData[row][entry].blobId] = blobData;
			}
			else
			{
				if(imgData[row][entry].min < blobs[imgData[row][entry].blobId].min.x)
					blobs[imgData[row][entry].blobId].min.x = imgData[row][entry].min;
				else if(imgData[row][entry].max > blobs[imgData[row][entry].blobId].max.x)
					blobs[imgData[row][entry].blobId].max.x = imgData[row][entry].max;
				if(row < blobs[imgData[row][entry].blobId].min.y)
					blobs[imgData[row][entry].blobId].min.y = row;
				else if(row > blobs[imgData[row][entry].blobId].max.y)
					blobs[imgData[row][entry].blobId].max.y = row;
			}
		}
	}

	// Calculate center
	for(map<unsigned int, blob>::iterator i = blobs.begin(); i != blobs.end(); ++i)
	{
		(*i).second.center.x = (*i).second.min.x + ((*i).second.max.x - (*i).second.min.x) / 2;
		(*i).second.center.y = (*i).second.min.y + ((*i).second.max.y - (*i).second.min.y) / 2;

		int size = ((*i).second.max.x - (*i).second.min.x) * ((*i).second.max.y - (*i).second.min.y);

		// Print coordinates on image, if it is large enough
		if(size > 800)
		{
			CvFont font;
			cvInitFont(&font, CV_FONT_HERSHEY_PLAIN, 1.0, 1.0, 0, 1, CV_AA);
			char textBuffer[128];

			// Draw crosshair and print coordinates (just for debugging, not necessary for later multi-touch use)
			cvLine(finalFrame, cvPoint((*i).second.center.x - 5, (*i).second.center.y), cvPoint((*i).second.center.x + 5, (*i).second.center.y), cvScalar(0, 0, 153), 1);
			cvLine(finalFrame, cvPoint((*i).second.center.x, (*i).second.center.y - 5), cvPoint((*i).second.center.x, (*i).second.center.y + 5), cvScalar(0, 0, 153), 1);
			sprintf(textBuffer, "(%d, %d)", (*i).second.center.x, (*i).second.center.y);
			cvPutText(finalFrame, textBuffer, cvPoint((*i).second.center.x + 5, (*i).second.center.y - 5), &font, cvScalar(0, 0, 153));
			cvRectangle(finalFrame, cvPoint((*i).second.min.x, (*i).second.min.y), cvPoint((*i).second.max.x, (*i).second.max.y), cvScalar(0, 0, 153), 1);

			// Show center point
			//cout << "(" << (*i).second.center.x << ", " << (*i).second.center.y << ")" << endl;
		}
	}
}

int main()
{
	CvCapture * capture = cvCaptureFromCAM(CV_CAP_ANY);
	if(!capture)
	{
		fprintf( stderr, "ERROR: capture is NULL \n" );
		getchar();
		return -1;
	}

	// Create a window in which the captured images will be presented
	//cvNamedWindow( "Capture", CV_WINDOW_AUTOSIZE );
	cvNamedWindow("Capture", 0);
	cvNamedWindow("Result", 0);

	while(1)
	{	
		// Get one frame from the web cam
		IplImage* frame = cvQueryFrame(capture);
		if(!frame)
		{
			fprintf( stderr, "ERROR: frame is null...\n" );
			getchar();
			break;
		}

		IplImage* gsFrame;
		IplImage* finalFrame;
		gsFrame = cvCreateImage(cvSize(frame->width,frame->height), IPL_DEPTH_8U, 1);
		finalFrame = cvCloneImage(frame);
		
		// Convert image to grayscale
		cvCvtColor(frame, gsFrame, CV_BGR2GRAY);
		
		// Blur the images to reduce the false positives
		cvSmooth(gsFrame, gsFrame, CV_BLUR);
		cvSmooth(finalFrame, finalFrame, CV_BLUR);

		// Detection (with timer for debugging purposes)
		clock_t start = clock();
		detectBlobs(gsFrame, finalFrame);
		clock_t end = clock();
		cout << end-start << endl;

		// Show images in a nice window
		cvShowImage( "Capture", frame );
		cvShowImage( "Result", finalFrame );
		// Do not release the frame!

		cvReleaseImage(&gsFrame);
		cvReleaseImage(&finalFrame);

		//If ESC key pressed, Key=0x10001B under OpenCV 0.9.7(linux version),
		//remove higher bits using AND operator
		if( (cvWaitKey(10) & 255) == 27 ) break;
	}

	// Release the capture device housekeeping
	cvReleaseCapture( &capture );
	cvDestroyWindow( "Capture" );
	cvDestroyWindow( "Result" );
	return 0;
}
