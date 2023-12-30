/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

$commons = function() {

	function loadBlob(url, callback) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (this.readyState == 4 && this.status == 200) {
				callback(this.response);
			}
		}
		xhr.open('GET', url);
		xhr.responseType = 'blob';
		xhr.send();
	}

	function loadJsonFile(url, callback) {
		$.ajax({
			'type' : 'get',
			'url' : url,
			'dataType' : 'json',
			'success' : function(data) {
				callback(data);
			},
			'error' : function(XMLHttpRequest, textStatus, errorThrown) {
				console.error(textStatus + ' (' + XMLHttpRequest.status + '): ' + errorThrown);
			}
		});
	}

	function dataURLtoBlob(dataurl) {
		var arr = dataurl.split(','), mime = arr[0].match(/:(.*?);/)[1], bstr = atob(arr[1]), n = bstr.length, u8arr = new Uint8Array(
				n);
		while (n--) {
			u8arr[n] = bstr.charCodeAt(n);
		}
		return new Blob([ u8arr ], {
			type : mime
		});
	}

	function blobToDataURL(blob, callback) {
		var reader = new FileReader();
		reader.onload = function(e) {
			callback(e.target.result);
		}
		reader.readAsDataURL(blob);
	}

	function getParam(url, key) {
		var pairs = new URL(url).search.substring(1).split('&');
		for (i in pairs) {
			var split = pairs[i].split('=');
			if (key == split[0]) {
				return decodeURIComponent(split[1]);
			}
		}
	}

	function loading(fShow) {
		if (fShow) {
			if ($('#loading').size() == 0) {
				$('<div>', {
					'id' : 'loading',
					'css' : {
						'position' : 'fixed',
						'top' : '0px',
						'left' : '0px',
						'height' : '100%',
						'width' : '100%',
						'background-color' : '#FFF',
						'background-image' : 'url("../images/loading.gif")',
						'background-repeat' : 'no-repeat',
						'background-position' : 'center',
						'filter' : 'alpha(opacity = 85)',
						'-moz-opacity' : '0.85',
						'opacity' : '0.85'
					}
				}).appendTo($('body'));
			}
		} else {
			$('#loading').remove();
		}
	}

	return {
		'loadBlob' : loadBlob,
		'loadJsonFile' : loadJsonFile,
		'dataURLtoBlob' : dataURLtoBlob,
		'blobToDataURL' : blobToDataURL,
		'getParam' : getParam,
		'loading' : loading
	};
}();