# script-replay-converter

Both **script** and **scriptreplay** allow you to record and replay your bash sessions, as described at
https://www.redhat.com/sysadmin/playback-scriptreplay.

The format, splitted into two files, is not convenient to edit/fix/annotate the recording, to remove confidential information.  

The tool **script-replay-converter** enable you to convert "script bi-file format" into a "json format", back and forth, so that it’s easier to edit the session by updating quickly the easier to update json format.

## Capture script: 

Capture a terminal session:
```
script --t=script_log -q scriptfile
# do something
# and then type exit
```

## Convert scripts to json format:

Convert the *script_log* and *scriptfile* files into *script.json*:
```
./mill convert.run tojson script_log scriptfile script.json
```

## Edit the text session 

Edit directly the script.json file, the entries list is far easier to edit:
```
   [1.66147, "./mill converter.run"],
   [0.816811, "\r\n"],
```

## Convert back json to script format

Convert back the edited *script.json* into *script_log_edited* and *scriptfile_edited* files:
```
./mill convert.run fromjson script_log_edited scriptfile_edited script.json
```

## Replay script:

Replay the edited script:
```
scriptreplay --timing=script_log_edited scriptfile_edited
```

## Prerequise

Java is a prerequisite. 

The *mill* script is provided in this repository. You can fetch it directly from https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html#_installation.

## Licenses

mill: https://github.com/com-lihaoyi/mill/blob/main/LICENSE

zio & zio-stream: https://github.com/zio/zio/blob/master/LICENSE

zio-json: https://github.com/zio/zio-json/blob/develop/LICENSE
