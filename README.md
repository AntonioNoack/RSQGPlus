# RSQGPlus
A tool to see [your G+ profile offline](https://anionoa.phychi.com/+), with the downloaded data from takeout.google.com, in a similar style.

![alt text](https://raw.githubusercontent.com/AntonioNoack/RSQGPlus/master/raw/img/promoImage.png)

**How to?**

Download your "Stream in Google+" data from [Google Takeout](https://takeout.google.com), then unzip it into some directory where you have at least 10 MB of additional space available.

Then compile or download [the program](https://anionoa.phychi.com/+/RSQGPlus8080.jar). Start it, best from console ("java -jar RSQGPlus8080.jar") to see the log, and select one file from the folder with your posts inside the Stream in G+ folder. There should be .json, .csv, and .png/.jpg files inside! Then select the port it should be running on. My recommendation is 80 for Windows and 8080 for Linux.

It will create a cache folder inside that folder and download all additional needed images, e.g. profile pictures.

When the status switches to "Ready", you can visit http://localhost:8080 (or the port you selected) and should see your G+ profile on your local computer, even without internet access! :D

Enjoy!

**How can local other phones etc see my profile?**

You've to find out [your local IP](https://lifehacker.com/how-to-find-your-local-and-external-ip-address-5833108) and type that into the url bar with :8080 (or the port you selected) as port behind.

**How can I change the directory of my data?**

Either remove that folder and the program should ask you itself, or remove the config file / change it's content located at your home directory/.AntonioNoack/RSQGPlus/config.txt.

**How can I thank you?**

Maybe donate a bit, which is equal to making me a little present / paying me a coffee (not liking coffee yet XD) / chocolate bar (liking that a lot better :D) :)

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](antonio-noack@gmx.de)
