document.getElementById("import").addEventListener("change", function() {
  var file = document.getElementById("import").files[0];
  var reader = new FileReader();
  reader.onload = function(e) {
    editor.setValue(JSON.parse(e.target.result));
  }
  reader.readAsText(file);
});

// customize
JSONEditor.defaults.resolvers.unshift(function(schema) {
  if(schema.type === "string" && schema.media && schema.media.binaryEncoding==="text") {
    return "textfile";
  }
});

JSONEditor.defaults.editors.textfile = JSONEditor.AbstractEditor.extend({
  getNumColumns: function() {
    return 4;
  },
  build: function() {    
    var self = this;
    this.title = this.header = this.label = this.theme.getFormInputLabel(this.getTitle());

    this.input = this.theme.getFormInputField('hidden');
    this.container.appendChild(this.input);
    
    // Don't show uploader if this is readonly
    if(!this.schema.readOnly && !this.schema.readonly) {
      if(!window.FileReader) throw "FileReader required for base64 editor";
      
      // File uploader
      this.uploader = this.theme.getFormInputField('file');
      
      this.uploader.addEventListener('change',function(e) {
	e.preventDefault();
	e.stopPropagation();
        
	if(this.files && this.files.length) {
	  var fr = new FileReader();
	  fr.onload = function(evt) {
	    self.value = evt.target.result;
	    self.refreshPreview();
	    self.onChange(true);
	    fr = null;
	  };
	  fr.readAsText(this.files[0]);
	}
      });
    }

    this.preview = this.theme.getFormInputDescription(this.schema.description);
    this.container.appendChild(this.preview);

    this.control = this.theme.getFormControl(this.label, this.uploader||this.input, this.preview);
    this.container.appendChild(this.control);
  },
  refreshPreview: function() {
    if(this.last_preview === this.value) return;
    this.last_preview = this.value;
    
    this.preview.innerHTML = '';
    
    if(!this.value) return;
    
    this.preview.innerHTML = '<strong>Size:</strong> '+parseInt(this.value.length/1024*10)/10+"kbytes";
  },
  enable: function() {
    if(this.uploader) this.uploader.disabled = false;
    this._super();
  },
  disable: function() {
    if(this.uploader) this.uploader.disabled = true;
    this._super();
  },
  setValue: function(val) {
    if(this.value !== val) {
      this.value = val;
      this.input.value = this.value;
      this.refreshPreview();
      this.onChange();
    }
  },
  destroy: function() {
    if(this.preview && this.preview.parentNode) this.preview.parentNode.removeChild(this.preview);
    if(this.title && this.title.parentNode) this.title.parentNode.removeChild(this.title);
    if(this.input && this.input.parentNode) this.input.parentNode.removeChild(this.input);
    if(this.uploader && this.uploader.parentNode) this.uploader.parentNode.removeChild(this.uploader);

    this._super();
  }
});

// Initialize the editor with a JSON schema
var editor = new JSONEditor(document.getElementById('editor_holder'),{
  schema: {
    "title": "2D localization data",
    "type": "object",
    "properties": {
      "anchor": {
	"type": "object",
	"title": "Anchor",
	"properties": {
	  "latitude": {
	    "type": "number",
	    "title": "Latitude"
	  },
	  "longitude": {
	    "type": "number",
	    "title": "Longitude"
	  },
	  "rotate": {
	    "type": "number",
	    "title": "Rotate"
	  }
	}
      },
      "samples": {
	"type": "array",
	"format": "table",
	"title": "Samples",
	"uniqueItems": true,
	"items": {
	  "type": "object",
	  "title": "Sample",
	  "properties": {
	    "data": {
	      "type": "string",
	      "title": "Data file",
	      "media": {
		"binaryEncoding": "text"
	      }
	    }
	  }
	}
      },
      "beacons": {
	"type": "array",
	"format": "table",
	"title": "Beacons",
	"uniqueItems": true,
	"items": {
	  "type": "object",
	  "title": "Beacon",
	  "properties": {
	    "data": {
	      "type": "string",
	      "title": "Data file",
	      "media": {
		"binaryEncoding": "text"
	      }
	    }
	  }
	}
      },
      "layers": {
	"type": "array",
	"title": "Layers",
	"format": "table",
	"uniqueItems": true,
	"items": {
	  "type": "object",
	  "title": "Layer",
	  "properties": {
	    "data": {
	      "type": "string",
	      "title": "Map image",
	      "media": {
		"binaryEncoding": "base64"
	      }
	    },
	    "param": {
	      "type": "object",
	      "title": "Parameters",
	      "properties": {
		"ppmx": {
		  "type": "number"
		},
		"ppmy": {
		  "type": "number"
		},
		"ppmz": {
		  "type": "number",
		  "default": 1
		},
		"originx": {
		  "type": "number"
		},
		"originy": {
		  "type": "number"
		},
		"originz": {
		  "type": "number",
		  "default": 0
		}
	      }
	    }
	  }
	}
      },
      "OrientationMeterAverageParameters": {
	"type": "object",
	"properties": {
	  "ratioAveraging": {"type": "number", "default": 0.1}
	}
      },
      "PedometerWalkingStateParameters": {
	"type": "object",
	"properties": {
	  "walkDetectStdevWindow": {"type": "number", "default": 0.8},
	  "walkDetectSigmaThreshold": {"type": "number", "default": 0.6},
	  "nStepConst": {"type": "number", "default": 0.1},
	  "queue_limit": {"type": "number", "default": 80},
	  "queue_min": {"type": "number", "default": 80}
	}
      },
      "PoseRandomWalkerInBuildingProperty": {
	"type": "object",
	"properties": {
	  "probabilityUp": {"type": "number", "default": 0.25},
	  "probabilityDown": {"type": "number", "default": 0.25},
	  "probabilityStay": {"type": "number", "default": 0.5},
	  "wallCrossingAliveRate":  {"type": "number", "default": 1.0},
	  "maxTrial": {"type": "number", "default": 1}
	}
      },
      "ObservationModelParameters": {
	"type": "string",
	"media": {
	    "binaryEncoding": "text"
	}
      },
      "alphaWeaken": {
	"type": "number",
	"default": 0.5
      },
      "nStrongestBeacons": {
	"type": "number",
	"default": 10
      }
    }
  }
});

function downloadFile(data, filename) {    
  var blob = new Blob([data], { type: 'text/json;charset=utf-8;' });
  if (navigator.msSaveBlob) {
    navigator.msSaveBlob(blob, filename);
  } else {
    var link = document.createElement("a");
    if (link.download !== undefined) {
      var url = URL.createObjectURL(blob);
      link.setAttribute("href", url);
      link.setAttribute("download", filename);
    } else {        
      link.href = 'data:attachment/json,' + data;
    }
    link.style = "visibility:hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
// Hook up the submit button to log to the console
document.getElementById('submit').addEventListener('click',function() {
  // Get the value from the editor
  console.log(editor.getValue());
  downloadFile(JSON.stringify(editor.getValue(),null, "\t"), "2dMapData.json");
});
