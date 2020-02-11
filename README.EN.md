# BitchMachine
A tool to grab red envelope in wechat

English

[Chinese](README.md)

# How to use

1. Install app
2. Open accessibility service of this app
3. Kepp screen on
3. wait for a red envelope in wechat

# Implementation

Android system allows user apps to monitor Notifications and Window Content Change Events of apps with specific package names. 
So, When these events happens:

- find notifications with red envelope key words
- find messages with red envelope key words in chat list
- find messages with red envelope key words in chat detail

Performing click actions to grab that.

WARNING: For technical study only, avoid commercial using