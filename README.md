# Bluetooth Control of Electrical Outlets
An android app and arduino program that can be used to control electrical outlets from your phone. The app is named "Rufus". You can find the Android code in the folder marked 'Rufus_android' and the arduino code in the folder marked 'Rufus_arduino'.

Here's a video I made showing what the app and device do (https://www.youtube.com/watch?v=70l2b662KCY) and you can also find more information on my personal webpage (http://www.jeffmoskowitz.com/engineering).

This project uses an android smartphone, a surge protector, relays, a bluetooth module and an arduino to control a number of electrical outlets remotely. It functions as a kind of universal remote for electronic devices.

If you want to recreate this project yourself, you'll need android studio and the Arduino IDE. Import this code into Android Studio and make the following changes:

1. Change the center icon and launcher icon (in the res folder) to whatever you desire. In my version of the app I used a great icon I had lying around from another project. But since I didn't design that icon, I couldn't make it freely available online and instead have replaced it with a simple groucho marx face on this repository.

2. Change the UUID and MAC Address to whatever is appropriate for your device. I've highlighted these two variables in the beginning of the main app code.

If you have any questions, feel free to ask.
