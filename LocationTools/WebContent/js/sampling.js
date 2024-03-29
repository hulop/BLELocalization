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

	function normalize(s) {
		return Math.round(parseFloat(s)*100)/100;
	}

	var mapView = null, floorList = [], filterRefid;
	var sampleref;
	var ref_to_floor = {}

	$(document).ready(function() {
		$('button').button();
		$('.btn').button();
		var MV = DefaultMapView;
		mapView = new MV('mapdiv', {
			'baseURL' : "./",
			'floors_ready' : function() {
				refreshFloorList();
			},
			'click' : function(x, y) {
				x = parseInt(x*100)/100;
				y = parseInt(y*100)/100;
				if ($('#ref_x').is(':visible')) {
					$('#ref_x').val(x);
					$('#ref_y').val(y);
					showFormMarker(x, y);
				}
				if ($('#sample_x').is(':visible') && sampleref) {
					var p = inverse(x, y, sampleref);
					$('#sample_x').val(p.x);
					$('#sample_y').val(p.y);
					showFormMarker(x, y);
				}
			},
			'featureclick' : function(e) {
				console.log('featureclick');
				var options = (e.feature.data && e.feature.data.options) || {};
				if (options.sampling) {
					showSampleForm(options.sampling, options._ref);
				} else if (options.refpoint) {
					showRefForm(options.refpoint);
				}
			}
		});
		$('#ref_floor').on('change', function(e) {
			var floor = floorList[this.value];
			mapView.clearMarkers();
			mapView.loadFloor(floor.floorInfo);
		});
		refresh();
		refreshFloorList();
	});

	/*
	 * Sampling Data
	 */
	var sampleDataType = 'samplings';

	function createSampleData(form) {
		var $form = $(form);
		var metadata = {
			'name' : $form.find('#name').val() || '',
			'comment' : $form.find('#comment').val() || ''
		};
		var id = $form.find('#sample_id').val();
		if (id) {
			var x = $form.find('#sample_x').val(), y = $form.find('#sample_y').val(), z = $form.find('#sample_z').val();
			if (isNaN(x) || isNaN(y) || isNaN(z)) {
				$('#message').text('Invalid x, y or z.');
				return;
			}
			var tags = $form.find('#sample_tags').val().split(/[\n]/).filter(function(tag) {
				return tag.length != 0;
			});
			;
			var query = {
				'_id' : {
					'$oid' : id
				}
			}, update = {
				'$set' : {
					'_metadata.name' : metadata.name,
					'_metadata.comment' : metadata.comment,
					'information.tags' : tags,
					'information.x' : parseFloat(x),
					'information.y' : parseFloat(y),
					'information.z' : parseFloat(z)
				}
			};
			dataUtil.postData({
				'type' : sampleDataType,
				'data' : {
					'action' : 'update',
					'query' : JSON.stringify(query),
					'update' : JSON.stringify(update)
				},
				'success' : function(data) {
					console.log(data);
					hideSampleForm();
					refresh();
				},
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});
			return;
		}
		var data = new FormData(form);
		data.append('_metadata', JSON.stringify(metadata));
		dataUtil.postFormData({
			'type' : sampleDataType,
			'data' : data,
			'success' : function(data) {
				console.log(data._id);
				hideSampleForm();
				refresh();
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function refreshSampleTable() {
		$('#data_table').empty();
		var query = {};
		if (filterRefid) {
			query['information.refid'] = filterRefid;
			$('#reset_filter').show();
		} else {
			$('#reset_filter').hide();
		}
		dataUtil.getData({
			'type' : sampleDataType,
			'data' : {
				'query' : JSON.stringify(query),
				'keys' : JSON.stringify({
					'_metadata' : 1,
					'information' : 1
				}),
				'limit' : 1000,
				'sort' : JSON.stringify({
					'_id' : -1
				})
			},
			'success' : function(data) {
				console.log(data);
				createSampleTable(data).appendTo('#data_table').dataTable({
					'stateSave': true
				});
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function createSampleTable(data) {
		var table = $('<table>', {
			'id' : 'sample_table',
			'class' : 'display',
			'cellpadding' : 0,
			'cellspacing' : 0,
			'border' : 0,
			'data-order' : '[[ 0, "desc" ]]'
		});
		var head = createTableRow([ 'created_at', 'name', 'comment', 'tags', 'site_id', 'floor', 'floor_num', 'x', 'y', 'z', 'absx', 'absy', 'action' ], '<th>');
		$('<thead>').append(head).appendTo(table);
		var tbody = $('<tbody>').appendTo(table);
		$('<tfoot>').append(head.clone()).appendTo(table);
		data.forEach(function(obj, i) {
			var id = obj._id.$oid;
			var info = obj.information || {};
			var md = obj._metadata || {};

			var actions = $('<span>', {
				'css' : {
					'white-space' : 'nowrap'
				}
			});

			if (info.floor) {
				actions.append(createButton('plot', function() {
					mapView.clearMarkers();
					var floorInfo = getFloorInfo(info);
					if (floorInfo) {
						scrollToMap();
						mapView.loadFloor(floorInfo, {
							'success' : function() {
								mapView.addMarker({
									'x' : info.absx,
									'y' : info.absy,
									'options' : {
										'sampling' : obj
									}
								});
							}
						});
					}
				}));
			}

			actions.append(createButton('edit', function() {
				showSampleForm(obj);
			}), createButton('delete', function() {
				deleteData(sampleDataType, id, "sample data \""+md.name+"\"");
			}), createLink('details', 'data/' + sampleDataType + '/' + id));

			var tags = (info.tags || []).join(', ');
			var created_at = md.created_at ? new Date(md.created_at.$date).toLocaleString() : '';
			tbody.append(createTableRow([ created_at, md.name, md.comment, tags, info.site_id, info.floor, info.floor_num, normalize(info.x), normalize(info.y), normalize(info.z), normalize(info.absx),
					normalize(info.absy), actions ], '<td>'));
		});
		return table;
	}

	function convert(x, y, ref) {
		var rad = ref.rotate / 180 * Math.PI;
		var cos = Math.cos(rad);
		var sin = Math.sin(rad);

		return {
			x: cos*x - sin*y + ref.x,
			y: sin*x + cos*y + ref.y
		};
	}

	function inverse(x, y, ref) {
		x -= ref.x;
		y -= ref.y;

		var rad = ref.rotate / 180 * Math.PI;
		var cos = Math.cos(rad);
		var sin = Math.sin(rad);

		return {
			x: cos*x - sin*y,
			y: sin*x + cos*y
		};
	}

	function showSampleForm(obj, ref) {
		hideRefForm();
		$('.sample_hide_edit').hide();
		if (obj) {
			$('#sampling_form .forCreate').hide();
			$('#sampling_form .forEdit').show();
		} else {
			$('#sampling_form .forCreate').show();
			$('#sampling_form .forEdit').hide();
		}
		//$('.sample_show_edit').show();
		$('#sampling_form').dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'close' : function() {
				hideSampleForm();
			}
		});

		console.log(obj);
		obj = obj || {};
		var md = obj._metadata || {};
		var form = $('#sampling_form form');
		var info = obj.information || {};
		form.find('#name').val(md.name || '');
		form.find('#comment').val(md.comment || '');
		form.find('#data').val('');
		form.find('#sample_id').val((obj._id && obj._id.$oid) || '');
		form.find('#sample_tags').val((info.tags || []).join('\n'));
		form.find('#sample_x').val(info.x || 0);
		form.find('#sample_y').val(info.y || 0);
		form.find('#sample_z').val(info.z || 0);
		sampleref = ref;
		$("#sample_map").click(function(e) {
			console.log([ref, $('#sample_x').val(), $('#sample_y').val()]);
			var p = convert($('#sample_x').val()-0, $('#sample_y').val()-0, ref);

			showFormMarker(p.x, p.y);
			e.preventDefault();
			return false;
		});

		mapView.clearMarkers();
		for ( var i = 0; i < floorList.length; i++) {
			var floorInfo = floorList[i].floorInfo;
			if (!obj._id || floorInfo.floor == info.floor) {
				//$('#ref_floor').val(i).change();
				//$('#ref_floor').val(i);
				console.log("ref_floor "+i);
				mapView.loadFloor(floorInfo, {
					'success' : function() {
						if (obj._id) {
							mapView.addMarker({
								'x' : info.absx,
								'y' : info.absy
							});
						}
					}
				});
				break;
			}
		}
		clearMapCache();
	}

	function hideSampleForm() {
		//$('.sample_show_edit').hide();
		if ($('#sampling_form').is(':visible')) {
			$('#sampling_form').dialog('close');
		}
		$('.sample_hide_edit').show();
		clearMapCache();
	}

	function drawSampleMarkers(ref) {
		refid = ref._id;
		dataUtil.getData({
			'type' : sampleDataType,
			'data' : {
				'query' : JSON.stringify({
					'information.refid' : refid
				}),
				'keys' : JSON.stringify({
					'_metadata' : 1,
					'information' : 1,
					'data.timestamp' : 1
				})
			},
			'success' : function(data) {
				console.log(data);
				var markers = [];
				data.forEach(function(s) {
					markers.push({
						'x' : s.information.absx,
						'y' : s.information.absy,
						'options' : {
							'sampling' : s,
							'_ref' : ref
						}

					});
				});
				mapView.addMarkers(markers);
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});

	}

	/*
	 * Reference Points
	 */
	var refDataType = 'refpoints';

	function createRefData(form) {
		var floorInfo = floorList[form.ref_floor.value].floorInfo;
		var data = {
			'refid': floorInfo._id,
			'filename': floorInfo.filename || "",
			'floor' : floorInfo.floor,
			'floor_num' : floorInfo.floor_num,
			'anchor_lat' : floorInfo.lat,
			'anchor_lng' : floorInfo.lng,
			'anchor_rotate' : floorInfo.rotate,
			'x' : parseFloat(form.ref_x.value),
			'y' : parseFloat(form.ref_y.value),
			'rotate' : parseFloat(form.ref_rotate.value),
		};
		if (isNaN(data.x) || isNaN(data.y)) {
			$('#message').text('Invalid x or y.');
			return;
		}
		if (form.ref_id.value) {
			data._id = {
				'$oid' : form.ref_id.value
			};
		}
		var metadata = {
			'name' : form.ref_name.value || '',
			'comment' : form.ref_comment.value || ''
		};
		dataUtil.postData({
			'type' : refDataType,
			'data' : {
				'data' : JSON.stringify(data),
				'_metadata' : JSON.stringify(metadata)
			},
			'success' : function(data) {
				console.log(data._id);
				hideRefForm();
				refresh();
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function refreshRefTable() {
		$('#ref_data_table').empty();
		dataUtil.getData({
			'type' : refDataType,
			'data' : {
				'sort' : JSON.stringify({
					'_id' : -1
				})
			},
			'success' : function(data) {
				console.log(data);
				createRefTable(data).appendTo('#ref_data_table').dataTable({
					'stateSave': true
				});
				refreshSampleTable();
			},
			'error' : function(xhr, text, error) {
				$('#message').text(error || text);
			}
		});
	}

	function createRefTable(data) {
		var table = $('<table>', {
			'id' : 'ref_table',
			'class' : 'display',
			'cellpadding' : 0,
			'cellspacing' : 0,
			'border' : 0,
			'data-order' : '[[ 0, "desc" ]]'
		});
		var head = createTableRow([ 'created_at', 'name', 'comment', 'floor', 'floor_num', 'x', 'y', 'rotate', 'sampling data', 'action' ], '<th>');
		$('<thead>').append(head).appendTo(table);
		var tbody = $('<tbody>').appendTo(table);
		$('<tfoot>').append(head.clone()).appendTo(table);

		data.forEach(function(obj, i) {
			var id = obj._id.$oid;
			if (obj.refid) {
				ref_to_floor[id] = obj.refid.$oid;
			}
			var md = obj._metadata || {};

			var sample_actions = $('<span>', {
				'css' : {
					'white-space' : 'nowrap'
				}
			});
			var actions = $('<span>', {
				'css' : {
					'white-space' : 'nowrap'
				}
			});

			if (obj.floor) {
				actions.append(createButton('plot', function() {
					mapView.clearMarkers();
					var floorInfo = getFloorInfo(obj);
					if (floorInfo) {
						scrollToMap();
						mapView.loadFloor(floorInfo, {
							'success' : function() {
								mapView.addMarker({
									'x' : obj.x,
									'y' : obj.y,
									'options' : {
										'refpoint' : obj
									}
								});
							}
						});
					}
				}));
				sample_actions.append(createButton('plot', function() {
					mapView.clearMarkers();
					var floorInfo = getFloorInfo(obj);
					if (floorInfo) {
						scrollToMap();
						mapView.loadFloor(floorInfo, {
							'success' : function() {
								mapView.addMarker({
									'x' : obj.x,
									'y' : obj.y,
									'type' : 'blue',
									'options' : {
										'refpoint' : obj
									}
								});
								drawSampleMarkers(obj);
							}
						});
					}
				}),
				createButton('wall', function() {
					drawWallImage(obj);
				})
				);
			}

			actions.append(createButton('edit', function() {
				showRefForm(obj);
			}), createButton('delete', function() {
				deleteData(refDataType, id, md.name);
			}), createLink('details', 'data/' + refDataType + '/' + id));

			sample_actions.append(createButton('filter', function() {
				filterRefid = obj._id;
				refreshSampleTable();
			}), createButton('delete', function() {
				deleteSamplesForRefid(obj._id);
				refreshSampleTable();
			}),	$('<input type="checkbox" name="export_'+id+'">').prop('refid', obj._id).prop('refname', md.name));

			var created_at = md.created_at ? new Date(md.created_at.$date).toLocaleString() : '';
			tbody.append(createTableRow([ created_at, md.name, md.comment, obj.floor, obj.floor_num, obj.x, obj.y, obj.rotate, sample_actions, actions ],
					'<td>'));
		});
		return table
	}
	
	function drawWallImage(obj) {
		var floorInfo = getFloorInfo(obj);
		console.log([obj, floorInfo]);
		var canvas = $("<canvas>").appendTo(document.body)[0];
		canvas.width = floorInfo.width;
		canvas.height = floorInfo.height;
		var ctx = canvas.getContext("2d");
		var img = new Image();
		img.src = floorInfo.imageURL;
		img.onload=function() {
			//ctx.drawImage(img,0,0);
			ctx.fillStyle = "rgb(0,0,0)";
			ctx.fillRect(0, 0, canvas.width, canvas.height);
			
			refid = obj._id;
			dataUtil.getData({
				'type' : sampleDataType,
				'data' : {
					'query' : JSON.stringify({
						'information.refid' : refid
					}),
					'keys' : JSON.stringify({
						'_metadata' : 1,
						'information' : 1
					})
				},
				'success' : function(data) {
					console.log(data);
					var markers = [];
					data.forEach(function(s) {
						var ppmx = floorInfo.ppm_x;
						var ppmy = -floorInfo.ppm_y;
						var ox = floorInfo.origin_x;
						var oy = floorInfo.height - floorInfo.origin_y;
						
						var x = s.information.absx;
						var y = s.information.absy;
						
						var nx = ox + x*ppmx;
						var ny = oy + y*ppmy;
						
						//console.log([ppmx, ppmy, ox, oy, x, y, nx, ny]);
						
						ctx.fillStyle = "rgb(255,255,255)";
						ctx.beginPath();
						ctx.arc(nx, ny, ppmx, 0, 2*Math.PI);
						ctx.fill()
					});
					mapView.addMarkers(markers);
				},
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});

		}
		
		
	}

	function showRefForm(obj) {
		hideSampleForm();
		console.log(obj);
		obj = obj || {};
		var metadata = obj._metadata || {};
		$('.ref_hide_edit').hide();
		//$('.ref_show_edit').show();
		$('#refpoint_form').dialog({
			'width' : 'auto',
			'height' : 'auto',
			'resizable' : false,
			'close' : function() {
				hideRefForm();
			}
		});
		mapView.clearMarkers();
		var form = $('#refpoint_form form')[0];
		for ( var i = 0; i < floorList.length; i++) {
			var floorInfo = floorList[i].floorInfo;
			if (!obj._id || floorInfo.floor == obj.floor) {
				//$('#ref_floor').val(i).change();
				$('#ref_floor').val(i);
				console.log("ref_floor "+i);
				mapView.loadFloor(floorInfo, {
					'success' : function() {
						if (obj._id) {
							mapView.addMarker({
								'x' : obj.x,
								'y' : obj.y
							});
						}
					}
				});
				break;
			}
		}
		form.ref_id.value = obj._id ? obj._id.$oid : '';
		form.ref_x.value = obj._id ? obj.x : '0';
		form.ref_y.value = obj._id ? obj.y : '0';
		form.ref_rotate.value = obj._id ? obj.rotate : '0';
		form.ref_name.value = metadata.name || '';
		form.ref_comment.value = metadata.comment || ''
		clearMapCache();
	}

	function hideRefForm() {
		//$('.ref_show_edit').hide();
		if ($('#refpoint_form').is(':visible')) {
			$('#refpoint_form').dialog('close');
		}
		$('.ref_hide_edit').show();
		clearMapCache();
	}

	/*
	 * Commons
	 */

	var buttons = {}, links = {};

	function createButton(text, onclick) {
		return (buttons[text] = buttons[text] || $('<button>', {
			'text' : text
		}).button()).clone().on('click', onclick);
	}

	function createLink(text, href) {
		return (links[text] = links[text] || $('<a>', {
			'text' : text,
			'target' : '_blank'
		}).button()).clone().attr('href', href);
	}

	function createTableRow(array, tag) {
		var row = $('<tr>');
		array.forEach(function(col) {
			row.append($(tag).append(col));
		});
		//		row.find('button,a').button();
		return row;
	}

	function deleteSamplesForRefid(oid, noconfirm, callback) {
		if (noconfirm || confirm('Delete ?')) {
			var query = JSON.stringify({
				"information.refid": {
					"$in": [oid]
				}
			});
			
			$.ajax('data?type=samplings&query=' + encodeURIComponent(query), {
				'method' : 'DELETE',
				'dataType' : 'json',
				'success': function(data) {
					console.log(["DELETE", data])
					callback ? callback() : refresh();
				},
				'error': function(xhr, text, error) {
					$('#message').text(error || text);
				}
			})	
		}
	}
	
	function deleteData(type, id, name) {
		if (confirm('Delete ' + (name || id) + '?')) {
			dataUtil.deleteData({
				'type' : type,
				'id' : id,
				'success' : function(data) {
					console.log(data);
					if (type == 'refpoints') {
						deleteSamplesForRefid({
							'$oid' : id
						}, true);
						return;
					}
					refresh();
				},
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});
		}
	}

	function refresh() {
		mapView.clearMarkers();
		$('#message').empty();
		refreshRefTable();
	}

	function refreshFloorList() {
		floorList = mapView.getFloorList();
		console.log(floorList);
		$('#ref_floor').empty();
		$('#ref_floor2').empty();
		if (floorList.length == 0) {
			return;
		}
		floorList.forEach(function(floor, i) {
			$('#ref_floor').append($('<option>', {
				'text' : floor.label,
				//				'selected' : i == 0,
				'value' : i
			}));
			$('#ref_floor2').append($('<option>', {
				'text' : floor.label,
				//				'selected' : i == 0,
				'value' : i
			}));
		});
		$('#ref_floor').val(0).change();
		$('#ref_floor2').val(0).change();
	}

	function getFloorInfo(obj) {
		var id = obj.refid && obj.refid.$oid;
		if (id) {
			id = ref_to_floor[id] || id;
			for ( var i = 0; i < floorList.length; i++) {
				var fi = floorList[i].floorInfo;
				if (fi._id && fi._id.$oid == id) {
					return fi;
				}
			}
		}
		console.error('no floor info');
	}

	function showFormMarker(x, y) {
		if (floorList && !isNaN(x) && !isNaN(y)) {
			mapView.clearMarkers();
			mapView.addMarker({
				'x' : x,
				'y' : y
			});
		}
	}

	function clearMapCache() {
		try {
			$('#mapdiv').children()[0].offsets = null; // FIXME: should I call map.updateSize()?
		} catch (e) {
		}
	}

	function resetFilter() {
		filterRefid = null;
		refreshSampleTable();
	}

	function scrollToMap() {
		$(document).scrollTop($('#mapdiv').offset().top);
	}

	function exportSamplings() {
		var names = [], refids = [];
		$('#ref_data_table input[type=checkbox]:checked').each(function(i, checkbox) {
			names.push(checkbox.refname);
			refids.push(checkbox.refid);
		});

		if (refids.length > 0) {
			var query = JSON.stringify({
				'information.refid' : {
					'$in' : refids
				}
			}), sort = JSON.stringify({
				'information.refid' : 1
			});
			var link = $('<a>', {
				'css' : {
					'visibility' : 'hidden'
				},
				'href' : 'data/' + sampleDataType + '?query=' + encodeURIComponent(query) + '&sort=' + encodeURIComponent(sort)
			});
			console.log(query);
			if (link.prop('download') !== undefined) { // feature detection
				// Browsers that support HTML5 download attribute
				link.attr('download', names.join('-') + '.json');
				(link.appendTo('body'))[0].click();
				link.remove();
			}
		} else {
			alert('No reference points selected');
		}
	}
	function exportCSVSamplings() {
		var names = [], refids = [];
		$('#ref_data_table input[type=checkbox]:checked').each(function(i, checkbox) {
			names.push(checkbox.refname);
			refids.push(checkbox.refid);
		});

		if (refids.length > 0) {
			var query = JSON.stringify({
				'information.refid' : {
					'$in' : refids
				}
			}), sort = JSON.stringify({
				'information.refid' : 1
			});
			var link = $('<a>', {
				'css' : {
					'visibility' : 'hidden'
				},
				'href' : 'data/' + sampleDataType + '?query=' + encodeURIComponent(query) + '&sort=' + encodeURIComponent(sort) + '&format=csv'
			});
			if (link.prop('download') !== undefined) { // feature detection
				// Browsers that support HTML5 download attribute
				link.attr('download', names.join('-') + '.csv');
				(link.appendTo('body'))[0].click();
				link.remove();
			}
		} else {
			alert('No reference points selected');
		}
	}
	function plotSamplings() {
		var names = [], refids = [];
		$('#ref_data_table input[type=checkbox]:checked').each(function(i, checkbox) {
			names.push(checkbox.refname);
			refids.push(checkbox.refid);
		});

		if (refids.length > 0) {
			refids.forEach(function(refid) {
				drawSampleMarkers({_id:refid});
			});
		} else {
			alert('No reference points selected');
		}
	}
	function exportAllSamplings() {
			var link = $('<a>', {
				'css' : {
					'visibility' : 'hidden'
				},
				'href' : 'data/' + sampleDataType
			});
			if (link.prop('download') !== undefined) { // feature detection
				// Browsers that support HTML5 download attribute
				link.attr('download', 'all.json');
				(link.appendTo('body'))[0].click();
				link.remove();
			}
		
	}
	function exportAllSamplingsCSV() {
		var link = $('<a>', {
			'css' : {
				'visibility' : 'hidden'
			},
			'href' : 'data/' + sampleDataType +'?format=csv'
		});
		if (link.prop('download') !== undefined) { // feature detection
			// Browsers that support HTML5 download attribute
			link.attr('download', 'all.csv');
			(link.appendTo('body'))[0].click();
			link.remove();
		}
	
	}
	function deleteRefpoints() {
		var refids = [];
		$('#ref_data_table input[type=checkbox]:checked').each(function(i, checkbox) {
			refids.push(checkbox.refid);
		});
		if (refids.length > 0) {
			function deleteTop() {
				if (refids.length > 0) {
					refid = refids.shift()
					dataUtil.deleteData({
						'type' : refDataType,
						'id' : refid.$oid,
						'success' : function(data) {
							console.log(data);
							deleteSamplesForRefid(refid, true, deleteTop);
						},
						'error' : function(xhr, text, error) {
							$('#message').text(error || text);
						}
					});
				} else {
					refresh();
				} 
			}
			confirm('Delete ' + refids.length + ' refpoints and samples?') && deleteTop()
		} else {
			alert('No reference points selected');
		}
	}
	
	function importSamplings(fileInput, postfix, checkImport) {
		var file = fileInput.files[0];
		if (!file) {
			return;
		}
		
		var floorInfo = floorList[$("#ref_floor2").val()].floorInfo;
		var data = {
			'filename': floorInfo.filename || "",
			'floor' : floorInfo.floor,
			'floor_num' : floorInfo.floor_num,
			'anchor_lat' : floorInfo.lat,
			'anchor_lng' : floorInfo.lng,
			'anchor_rotate' : floorInfo.rotate,
			'refid' : floorInfo._id,
			'x' : 0,
			'y' : 0,
			'rotate' : 0
		};
		var metadata = {
			'name' : floorInfo._metadata.name+postfix,
			'comment' : file.name
		};
		
		var fr = new FileReader();
		fr.onload = function() {						
			var input = JSON.parse(fr.result);
			console.log(input);
			if (!input.length || input.length == 0) {
				return;
			}
			input = input.filter(function(i) {
				return checkImport(i);
			});	
			if (input.length == 0) {
				return;
			}

			dataUtil.postData({
				'type' : refDataType,
				'data' : {
					'data' : JSON.stringify(data),
					'_metadata' : JSON.stringify(metadata)
				},
				'success' : function(data) {
					console.log(data._id);
					//todo					
					input.forEach(function(i) {
						var meta = {
							name: floorInfo._metadata.name+postfix,
							comment: ""
						}
						delete i._id; 
						i.information.refid = data._id;
						dataUtil.postData({
							'type' : sampleDataType,
							'data' : {
								'data' : JSON.stringify(i),
								'_metadata' : JSON.stringify(meta)
							},
							'success' : function(data) {
								console.log(data._id);
							}
						});					
						
					});
					refresh();
				},
				'error' : function(xhr, text, error) {
					$('#message').text(error || text);
				}
			});		
		}
		fr.readAsText(file);
		
		$(fileInput).val(null);	
	}
	
	function importJSONSamplings() {
		importSamplings($("#import_json_sample_file")[0], "_import", function(d) {
			if (d.beacons && d.information) {
				return d;
			}
			return false;
		});
	}
	
	function importLidarSamplings() {
		importSamplings($("#import_lidar_file")[0], "_LiDAR", function(d) {
			if ((d.data || d.accessibility) && d.information) {
				return d;
			}
			return false;
		});
	}