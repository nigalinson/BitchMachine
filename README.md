# BitchMachine
A tool to grab red envelope in wechat

English

[Chinese](README.CN.md)

# How to use

1. Install app
2. Open accessibility service of this app
3. Kepp screen on
3. wait for a red envelope in wechat

# Implementation

Android system allows user apps to monitor Notifications and Window Content Change Events of apps with some specific package names. 
So, When these events happens:

- achieve notifications with red envelope key words
- achieve messages with red envelope key words in chat list
- achieve messages with red envelope key words in chat detail

we'll key performing click actions until red envelope "achieveUI", Then find the button with key words '拆', and perform click on it.
after this, you need to rool back to chat list activity.

WARNING: For technical study only, not for commercial use