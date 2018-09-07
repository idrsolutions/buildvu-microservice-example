
# BuildVu Microservice Example API #

This document details the raw requests - for language specific wrappers / examples, see the [Usage section in the readme](README.md).

### Uploading ###
See HTTP and POST params below for list of parameters.
 - Option 1, Send file in POST request:
    - The encoding type should be multipart/form-data and provide a 'file' parameter containing the file data (content and filename).
    - POST params should be ```{"input": "upload"}```
    - Note: 'filename' is usually included automatically but may not be depending on the tool generating the request.  
 - Option 2, Link to file through a URL:
    - POST params should be ```{"input": "download", "url": "http://your.url"}```
    - Note: Server will try to parse filename from url, however will default to "document.pdf" if that is not possible.

**URL:** ```/buildvu```

**Method:** POST

**HTTP and POST Params:**
* **"input":** the type of input for the server to handle.
* **"url":** the url for the server to handle; specified with {"input": "download"}.
* **"file":** the file for the server to handle; specified with {"input": "upload"}.

**Example request:**

```
POST https://[URL]/buildvu
```

**File upload POST request contents:**

```POST https://[URL]/buildvu
POST / HTTP/1.1
Content-Type: multipart/form-data; boundary=foo_boundary

--foo_boundary
Content-Disposition: form-data; name="input"

upload
--foo_boundary

    Content-Disposition: form-data; name="file", filename="example.pdf";
    Content-Type: application/pdf
    [Content of example.pdf]

foo_boundary--
```

**Passing file URL to server POST request contents:**

```POST https://[URL]/buildvu
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded;
Content-Length: 54

input=download&url=http://your.domain/path/to/file.pdf
```

**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
    "uuid": [String]
} 
```

**POST Error Response(s):**
* **Code** = 400
* **Content** = ```{error: "Missing input type"}```

* **Code** = 400
* **Content** = ```{error: "Unrecognised input type"}```

* **Code** = 500
* **Content** = ```{error: "Error handling file"}```

* **Code** = 400
* **Content** = ```{error: "Missing file"}```

* **Code** = 500
* **Content** = ```{error: "Missing file name"}```

* **Code** = 500
* **Content** = ```{error: "Internal error"}```

* **Code** = 400
* **Content** = ```{error: "No url given"}```

___


### Conversion Status

Check the status of a conversion.

**URL:** ```/buildvu```

**Method:** GET

**Params:**
* **Required:** uuid = [String]
* **Optional:** none

**Example request:**

```GET https://[URL]/buildvu?uuid=[uuid]```


**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
    "state": [String], * Will be "queued", "downloading", "processing", "processed" or "error"
    "previewUrl": [String], * Only when state is processed
    "downloadUrl": [String], * Only when state is processed
    "error": [String] * Only when state is error
    "errorCode" [integer] * Only on internal conversion error. See Internal error codes below
}
```

**Internal Error Codes**
* 1050: libre office timeout
* 1070: process error
* 1100: other
* 1200: could not get file from url

**GET Error Response(s):**
* **Code** = 404
* **Content** = ```{error: "No uuid provided"}```

* **Code** = 404
* **Content** = ```{error: "Unknown uuid [uuid]"}```
___


### Download

The converted output is available as a .zip archive at:

**URL:** ```/output/[uuid]/[filename].zip```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename].zip```

___

### View

Results of the conversion are viewable at:

**URL:** ```/output/[uuid]/[filename]/index.html```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename]/index.html```

___
