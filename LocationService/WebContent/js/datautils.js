/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation
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

var dataUtil = (function() {

	function onSuccess() {
		console.log('success');
		console.log(arguments);
	}

	function onError() {
		console.log('error');
		console.log(arguments);
	}

	return {
		'getData' : function(options) {
			$.ajax('data/' + options.type +"?"+(new Date()).getTime(), {
				'method' : 'GET',
				'data' : options.data || {},
				'dataType' : 'json',
				'error' : function() {
					(options.error || onError).apply(this, arguments);
				},
				'success' : function() {
					(options.success || onSuccess).apply(this, arguments);
				}
			});
		},

		'deleteData' : function(options) {
			$.ajax('data/' + options.type + '/' + options.id, {
				'method' : 'DELETE',
				'dataType' : 'json',
				'error' : function() {
					(options.error || onError).apply(this, arguments);
				},
				'success' : function() {
					(options.success || onSuccess).apply(this, arguments);
				}
			});
		},

		'postData' : function(options) {
			$.ajax('data/' + options.type, {
				'method' : 'POST',
				'data' : options.data,
				'dataType' : 'json',
				'error' : function() {
					(options.error || onError).apply(this, arguments);
				},
				'success' : function() {
					(options.success || onSuccess).apply(this, arguments);
				}
			});
		},

		'postFormData' : function(options) {
			// note: data/type url rewrite does not work with multipart
			$.ajax('data?type=' + options.type + (options.id?"&id="+options.id:"") , {
				'method' : 'POST',
				'contentType' : false,
				'processData' : false,
				'data' : options.data,
				'dataType' : 'json',
				'error' : function() {
					(options.error || onError).apply(this, arguments);
				},
				'success' : function() {
					(options.success || onSuccess).apply(this, arguments);
				}
			});
		}
	};
})();
