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

var $lidar_import = function() {
	var group_name, floor_lat, floor_lng;

	function importSamplingsJSON(input) {
		var file = input.files[0];
		if (file) {
//			group_name = prompt(file.name + '\n\nPlease enter group name:', file.name.replace(/\.json$/, ''));
			if (group_name) {
				var reader = new FileReader();
				reader.addEventListener('load', function(e) {
					$commons.loading(true);
					loadEntries(JSON.parse(reader.result));
				});
				reader.readAsText(file);
			}
			input.value = '';
		}
	}

	function loadEntries(entries) {
		var entry = entries.shift();
		if (!entry) {
			console.log('All done - refreshing');
			$commons.loading(false);
			refresh();
			return;
		}
		console.log(entry.directory);
		var m, info_file, image_files = [], sampling_files = [];
		entry.files && entry.files.forEach(function(file) {
			var path = $commons.getParam(file.url, 'path');
			if (m = /^.*\/(projection2d\/projection2D_threshold.*\.png)$/.exec(path)) {
				image_files.push({
					'url' : file.url,
					'name' : m[1]
				});
			} else if (m = /^.*\/(samplings_iBeacon_(.*)_(splitted|splitted_discretized_.*).json)$/.exec(path)) {
				var isLiDARr = m[3] == 'splitted';
				sampling_files.push({
					'url' : file.url,
					'name' : m[1],
					'postfix' : isLiDARr ? '_LiDAR' : '_import',
					'checkImport' : function(d) {
						return (isLiDARr ? (d.data || d.accessibility) : d.beacons) && d.information;
					}
				});
			} else if (m = /^.*\/floorplan_info\.json$/.exec(path)) {
				info_file = file;
			}
		});
		if (!info_file || sampling_files.length == 0) {
			loadEntries(entries);
			return;
		}
		$commons.loadJsonFile(info_file.url, function(floor_info) {
			floor_info.floor_image = floor_info.floor_image || 'projection2d/projection2D_threshold10.png';
			floor_info.group = floor_info.group || group_name;
			floor_info.lat = floor_info.lat || floor_lat,
			floor_info.lng = floor_info.lng || floor_lng,
			console.log(floor_info);
			image_files = image_files.filter(function(file) {
				return file.name == floor_info.floor_image;
			});
			if (image_files.length != 1) {
				loadEntries(entries);
				return;
			}
			$commons.loadBlob(image_files[0].url, function(blob) {
				console.log(blob);
				$commons.blobToDataURL(blob, function(url) {
					var img = new Image();
					img.onload = function() {
						floor_info.img_width = img.width;
						floor_info.img_height = img.height;
						img = null;
						submitImage(blob, floor_info, function(floorInfo) {
							console.log(floorInfo);
							submitSamplings(sampling_files, floorInfo, function(data) {
								console.log(data);
								loadEntries(entries);
							});
						});
					};
					img.src = url;
				});
			});
		});
	}

	function submitImage(blob, floor_info, callback) {
		var formData = new FormData();
		formData.append('file', blob);
		dataUtil.postFormData({
			'type' : 'file',
			'data' : formData,
			'success' : function(data) {
				console.log([ 'file save success', data ]);
				submitFloorplan(data.filename, floor_info, callback);
			},
			'error' : function(xhr, text, error) {
				console.error(error || text);
			}
		});

	}

	function submitFloorplan(filename, floor_info, callback) {
		var metadata = {
			'name' : floor_info.floor_name || '',
			'comment' : floor_info.comment || ''
		};
		var data = {
			'group' : floor_info.group || '',
			'floor' : floor_info.floor_number || 0,
			'origin_x' : floor_info.origin_x || 0,
			'origin_y' : floor_info.origin_y || 0,
			'width' : floor_info.img_width,
			'height' : floor_info.img_height,
			'ppm_x' : floor_info.ppm_x || 1,
			'ppm_y' : floor_info.ppm_y || 1,
			'lat' : floor_info.lat || 0,
			'lng' : floor_info.lng || 0,
			'rotate' : floor_info.rotate || 0,
			'filename' : filename
		};
		console.log([ 'send data', metadata, data ]);
		dataUtil.postData({
			'type' : 'floorplans',
			'data' : {
				'data' : JSON.stringify(data),
				'_metadata' : JSON.stringify(metadata)
			},
			'success' : function(floorplan) {
				callback && callback(floorplan);
			},
			'error' : function(xhr, text, error) {
				console.error(error || text);
			}
		});
	}

	function submitSamplings(samplingFiles, floorInfo, callback) {
		var file = samplingFiles.shift();
		if (!file) {
			callback && callback('submitSamplings done');
			return;
		}
		$commons.loadJsonFile(file.url, function(input) {
			console.log([ file, input ]);
			input = input.filter(function(i) {
				return file.checkImport(i);
			});
			if (!input || input.length == 0) {
				submitSamplings(samplingFiles, floorInfo, callback);
				return;
			}
			var data = {
				'filename' : floorInfo.filename || '',
				'floor' : floorInfo._metadata.name || getFloorStr(floorInfo),
				'floor_num' : floorInfo.floor,
				'anchor_lat' : floorInfo.lat,
				'anchor_lng' : floorInfo.lng,
				'anchor_rotate' : floorInfo.rotate,
				'refid' : floorInfo._id,
				'x' : 0,
				'y' : 0,
				'rotate' : 0
			};
			var metadata = {
				'name' : floorInfo._metadata.name + file.postfix,
				'comment' : file.name
			};
			console.log([ data, metadata, input ]);
			dataUtil.postData({
				'type' : 'refpoints',
				'data' : {
					'data' : JSON.stringify(data),
					'_metadata' : JSON.stringify(metadata)
				},
				'success' : function(refpoint) {
					console.log(refpoint);
					var meta = {
						name : floorInfo._metadata.name + file.postfix,
						comment : ''
					}
					input = input.map(function(i) {
						delete i._id;
						i.information.refid = refpoint._id;
						return i;
					});
					dataUtil.postData({
						'type' : 'samplings',
						'data' : {
							'data' : JSON.stringify(input),
							'_metadata' : JSON.stringify(meta)
						},
						'success' : function(data) {
							console.log(data);
							submitSamplings(samplingFiles, floorInfo, callback);
						}
					});
				}
			});
		});
	}

	function getFloorStr(floor) {
		var n = floor.floor;
		if (n - 0 >= 0) {
			return floor.group + "-" + (n - 0 + 1) + "F";
		}
		if (n - 0 < 0) {
			return floor.group + "-B" + (-n) + "F";
		}
	}

	function showDialog() {
		$('#li_file').val('')
		$('#li_group').val('')
		$('#li_lat').val('0')
		$('#li_lng').val('0')
		$('#li_submit').prop('disabled', true)
		$('#lidar_import_dialog').dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'close' : function() {
				hideDialog();
			}
		});
	}

	function hideDialog() {
		if ($('#lidar_import_dialog').is(':visible')) {
			$('#lidar_import_dialog').dialog('close');
		}
	}

	$(document).ready(function() {
		$('#li_file').on('change', function(event) {
			var files = event.target.files;
			if (files.length > 0) {
				$('#li_group').val(files[0].name.replace(/\.json$/, ''));
				$('#li_submit').prop('disabled', false);
			} else {
				$('#li_group').val('');
				$('#li_submit').prop('disabled', true);
			}
		});
		$('#li_submit').on('click', function(event) {
			var file = $('#li_file')[0];
			var group = $('#li_group').val();
			var lat = $('#li_lat').val();
			var lng = $('#li_lng').val();
			if (file.files.length == 0 || group.length == 0 || isNaN(lat) || isNaN(lng)) {
				alert('Invalid parameters')
				return;
			}
			group_name = group;
			floor_lat = Number(lat);
			floor_lng = Number(lng);
			importSamplingsJSON(file)
			hideDialog();
		});
		$('#li_cancel').on('click', function(event) {
			hideDialog();
		});
	});

	return {
		'importSamplingsJSON' : importSamplingsJSON,
		'showDialog' : showDialog
	};
}();