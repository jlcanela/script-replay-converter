# script-replay-converter

## Capture script: 

```
script --t=script_log -q scriptfile
```

## Replay script:

```
scriptreplay --timing=script_log scriptfile
```

```
scriptreplay --timing=script_log1 scriptfile1
```

## Convert script:

```
./mill convert.run tojson script_log scriptfile script.json
./mill convert.run fromjson script_log scriptfile script.json
```
