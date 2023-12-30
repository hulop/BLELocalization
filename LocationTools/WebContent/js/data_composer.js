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

var refpoints, refpointsMap;
var floorplans, floorplansMap;
var refpointFloorplanMap;
var layers;
var $status, timer;
function showStatus(text, waiting) {
	if (timer) clearInterval(timer);
	if (!$status) $status = $("<div>").appendTo(document.body);
	$status.text(text?text:"");
	if (waiting) {
		timer = setInterval(function() {
			$status.text($status.text()+". ");
		}, 500);
	}
}
$(document).ready(function() {
	showStatus("loading refpoints", true);
	dataUtil.getAllData({
		type: "refpoints",
		error: function() {
			showStatus("Error loading refpoints", false);
		},
		success: function(response) {
			refpoints = response.sort(function(a,b){
				//return a._metadata.name.localeCompare(b._metadata.name);
				var ad = new Date(a._metadata.created_at.$date);
				var bd = new Date(b._metadata.created_at.$date);
				return ad.getTime() - bd.getTime();
			});
			refpointsMap = {};
			refpoints.forEach(function(rp) {
				refpointsMap[rp._id.$oid] = rp;
			});

			showStatus("loading floorplans", true);
			dataUtil.getAllData({
				type: "floorplans",
				error: function() {
					showStatus("Error loading floorplans", false);
				},
				success: function(response) {
					floorplansMap = {};
					floorplans = response;
					floorplans.forEach(function(fp) {
						fp.image = new Image();
						fp.image.src = "data/file/"+fp.filename;
						floorplansMap[fp._id.$oid] = fp;
					});
					refpointFloorplanMap = {};
					refpoints.forEach(function(rp) {
						refpointFloorplanMap[rp._id.$oid] = floorplansMap[rp.refid.$oid];
					});
					layers = {};
					var temp = $("<canvas>")[0];
					var size = 1000;
					temp.width = temp.height = size;
					var ctx = temp.getContext("2d");
					ctx.fillStyle = "white";
					ctx.fillRect(0, 0, size, size);
					
					
					
					floorplans.forEach(function(fp) {
						var f = parseInt(fp.floor);
						var ls = layers[f];
						if (!ls) {
							layers[f] = {
									floor_num: f,
									data: temp.toDataURL(),
									param: {
										originx: size/2,
										originy: size/2,
										originz: 0,
										ppmx: 0.1,
										ppmy: 0.1,
										ppmz: 0,
										floor: f
									}
							};
						}
					});
					showStatus();
					generateTable(refpoints);
					
					showStatus("counting samplings", true);
					countSamplings({'beacons': {'$exists' : true}}, function(counts) {
						refpoints.forEach(function(fp) {
							fp._dom.find("td:nth-child(5) span").text(counts[fp._id.$oid] || 0);
						});
						countSamplings({'data': {'$exists' : true}}, function(counts) {
							refpoints.forEach(function(fp) {
								fp._dom.find("td:nth-child(6) span").text(counts[fp._id.$oid] || 0);
							});
							$(".exbtn").attr("disabled", null);
							showStatus("Counting completed", false);
						});
					});
				}
			})	
		}
	});		
	floorplans = [];
	refpoints = [];
	generateTable([]);
});
function getAnchor(refpoint, full) {
	var fp = floorplansMap[refpoint.refid.$oid];
	if (full) {
		return {
			latitude:parseFloat(fp.lat),
			longitude:parseFloat(fp.lng), 
			rotate:parseFloat(fp.rotate)
		};
	}
	return {
		lat:parseFloat(fp.lat),
		lon:parseFloat(fp.lng), 
		rotate:parseFloat(fp.rotate)
	};
}
function convertBeacons(refpoints, refpoint) {
	var beacons = [];
	var refids = $("#refpoints_table input[type=checkbox]:checked").map(function(){return $(this).val()}).toArray();
	refpoints.filter(function(rp){
		return refids.indexOf(rp._id.$oid) > -1;
	}).forEach(function(rp) {
		var fp = refpointFloorplanMap[rp._id.$oid];
		
		if (fp && fp.beacons && fp.beacons.features) {
			fp.beacons.features.forEach(function(b) {
				if (b.properties.type != "beacon" || !b.properties.uuid) return;
				var local = {x:b.geometry.coordinates[0], y:b.geometry.coordinates[1]};
				if (fp.beacons._crs == 'epsg:3857' && fp.ppm_x && fp.ppm_y) {
					local.x = (local.x - fp.origin_x) / fp.ppm_x;
					local.y = (local.y - fp.origin_y) / fp.ppm_y;
				}
				var anchor1 = getAnchor(rp);
				var	anchor2 = getAnchor(refpoint);
				
				var global = xy2latlon(local, anchor1);
				var local2 = latlon2xy(global, anchor2);
				var global2 = xy2latlon(local2, anchor2);
			
				beacons.push({
					x: local2.x,
					y: local2.y,
					lat: global.lat,
					lng: global.lon,
					z: 0,
					floor: fp.floor,
					uuid:b.properties.uuid.toLowerCase(),
					major:b.properties.major,
					minor:b.properties.minor,
					power:b.properties.power
				});
			});
		}
	});
	var uniq = {};
	return beacons.filter(function(b) {
		var id = b.uuid.toLowerCase()+"-"+b.major+"-"+b.minor+"-"+b.x+"-"+b.y+"-"+b.floor;
		if (uniq[id]) {
			return false;
		}
		uniq[id] = b;
		return true;
	});
}
function csvBeacons(beacons) {
	return 'uuid,major,minor,x,y,z,floor,power\n' + beacons.map(function(b) {
		return [b.uuid,b.major,b.minor,b.x,b.y,b.z,b.floor,b.power].join(",")+"\n";
	}).join("");
}
function filterRefpoints() {
	var text = $("#refFilterText").val();
	$("#refpoints_table tr:nth-child(n+2)").map(function(i, tr) {
		var b = text=="" || $(tr).find("td:nth(1)").text().match(text);
		$(tr).css("opacity",b?1.0:0.5);
		$(tr).find("input[type=checkbox]").attr("disabled",!b);
	});
}
function generateTable(fps) {
	$("#left").empty();
	$("#right").empty();
	
	var $div0 = $("<div>").appendTo($("#left")).append("<strong>Floor Plans</strong>");
	$div0.append("<button id='select_all' onclick='$(\"table input[type=checkbox]:not(:checked):not(:disabled)\").map(function(){return $(this).click()})'>Select all</button>");
	$div0.append("<button id='unselect_all' onclick='$(\"table input[type=checkbox]:checked:not(:disabled)\").map(function(){return $(this).click()})'>Unselect all</button>");
	$div0.append("<input id='refFilterText' type='text' onkeyup='filterRefpoints();'>");
	$div0.append("<button onclick='$(\"#refFilterText\").val(\"\");filterRefpoints();'>clear</button>")
	
	var $table = $("<table id='refpoints_table'>").appendTo($div0);
	
	generateTR(["Export","Reference point","Floor plan","Create","#Samples", "#Raw", "#Beacons", "Anchor for export"]).appendTo($table);
	fps.forEach(function(fp) {
		var $check = $("<input type='checkbox'>").val(fp._id.$oid).attr("checked","checked");
		
		var $label = $("<label>").attr("for", fp._id.$oid).text(fp._metadata.name);
		
		var samples = [];
		var beacons = (floorplansMap[fp.refid.$oid] && floorplansMap[fp.refid.$oid].beacons && floorplansMap[fp.refid.$oid].beacons.features) || [];
		var $nums = $("<span>").text(`${samples.length}`);
		var $numr = $("<span>").text(`${samples.length}`);
		var $numb = $("<span>").text(`${beacons.length}`);
		var $radio = $("<input type='radio'>").attr("name", "anchor").val(fp._id.$oid);
		//var $image = $("<img>").attr("src", "data/file/"+floorplansMap[fp.refid.$oid].filename)
		//				.css("max-width","120px").css("max-height","120px");
		fp._dom = generateTR([$check, $label, fp.floor, new Date(fp._metadata.created_at.$date).toLocaleString(), $nums, $numr, $numb, $radio]).appendTo($table);
	});
	var $div1 = $("<div>").appendTo($("#left")).append("<strong>Samples</strong>");
	$div1.append(
		$('<button>', {
			'text' : 'Export GeoJSON',
			'on' : {'click' : function() {
				exportSamplings('GeoJSON', `samplings_${(new Date()).getTime()}.geojson`);
			}}
		}),
		$('<button>', {
			'text' : 'Export JSON',
			'on' : {'click' : function() {
				exportSamplings('JSON', `samplings_${(new Date()).getTime()}.json`);
			}}
		}),
		$('<button>', {
			'text' : 'Export CSV',
			'on' : {'click' : function(event) {
				exportSamplings('CSV', `samplings_${(new Date()).getTime()}.csv`, (event.ctrlKey || event.metaKey) ? '&uuid=true' : '');
			}}
		}));
	
	var $div2 = $("<div>").appendTo($("#left")).append("<strong>Beacons</strong>");
	function convertedBeacons() {
		var refid = $("input[name=anchor]:checked").val();
		if (!refid) {
			alert("Need to select an anchor of floor plan for export");
			return null;
		}
		return convertBeacons(refpoints, refpointsMap[refid]);
	}
	$('<button>', {
		'text' : 'Export GeoJSON',
		'on' : {
			'click' : function() {
				var converted = convertedBeacons();
				// console.log(converted);
				if (converted) {
					var geojson = {
						'type' : 'FeatureCollection',
						'features' : converted.map(function(obj) {
							return {
								'type' : 'Feature',
								'geometry' : {
									'type' : 'Point',
									'coordinates' : [ obj.lng, obj.lat ]
								},
								'properties' : obj
							};
						})
					};
					downloadFile(JSON.stringify(geojson), 'json', `beacons_${(new Date()).getTime()}.geojson`);
				}
			}
		}
	}).appendTo($div2);
	$('<button>', {
		'text' : 'Export JSON',
		'on' : {
			'click' : function() {
				var converted = convertedBeacons();
				// console.log(converted);
				if (converted) {
					downloadFile(JSON.stringify(converted), 'json', `beacons_${(new Date()).getTime()}.json`);
				}
			}
		}
	}).appendTo($div2);
	var $button = $("<button>Export CSV</button>").appendTo($div2);
	$button.click(function() {
		var converted = convertedBeacons();
		//console.log(converted);
		if (converted) {
			downloadFile(csvBeacons(converted), "csv",
						`beacons_${(new Date()).getTime()}.csv`);
		}
	});
	
	var $div3 = $("<div>").appendTo($("#right")).append("<strong>Layers</strong>");
	var $table2 = $("<table>").appendTo($div3);
	var $opt1 = $("<input type='radio'>").val("none").attr("name", "mm_type").attr("id","mm_none").attr("checked","checked").appendTo($div3);
	var $lab1 = $("<label>").attr("for","mm_none").text("No map matching").appendTo($div3);
	$div3.append("<br>");
	var $opt2 = $("<input type='radio'>").val("geojson").attr("name", "mm_type").attr("id","mm_geojson").appendTo($div3);
	var $lab2 = $("<label>").attr("for","mm_geojson").text("Generate map matching image from pedestrian network (geoJSON)").appendTo($div3);
	$div3.append("<br>");
	var $div4 = $("<div style='padding-left:30px'>").appendTo($div3);
	$("<input type='checkbox' checked id='ch_drawimages'><label for='ch_drawimages'>Draw Image</label>").appendTo($div4);
	$div4.append("<br>");
	$("<input type='checkbox' checked id='ch_drawlinks'><label for='ch_drawlinks'>Draw Links</label>").appendTo($div4);
	$div4.append("<br>");
	$("<input type='checkbox' checked id='ch_drawsamples'><label for='ch_drawsamples'>Draw Samples</label>").appendTo($div4);
	$div4.append("<br>");	
	$("<input type='number' id='default_width' value='5'><label for='default_width'>Default Link Width (meters)</label>").appendTo($div4);
	$div4.append("<br>");	
	
	var $mmfile = $("<input type='file'>").css("display", "none").appendTo($div3);
	//var $mmbtn = $("<button>").css("display", "none").text("Import").appendTo($div3);
	$mmfile.change(function() {
		var refid = $("input[name=anchor]:checked").val();
		if (!refid) {
			alert("Need to select an anchor of floor plan for export");
			$mmfile.val("");
			return;
		}
		if ($mmfile[0].files[0]) {
			var fr = new FileReader();
			fr.onload = function() {
				var geojson = JSON.parse(fr.result);
				infoSamplings($("#ch_drawsamples")[0].checked, function(data) {
					for(var k in layers) {
						var l = layers[k];
						generateMMImage(l, geojson, refpointsMap[refid], data);
						$("#layer"+k).attr("src", l.data);
					}
				});
			};			
			fr.readAsText($mmfile[0].files[0]);
		} 
		else {
			alert("no file is specified");	
		}
	});
	$("input[name=mm_type]").change(function(e) {	
		var selected = ($(this).val() == "geojson");
		$mmfile.css("display", selected?"inline":"none");
		//$mmbtn.css("display", selected?"inline":"none");
	});
	
	if (layers) {
		generateTR(["Level", "image"]).appendTo($table2);
		Object.keys(layers).sort().forEach(function(k){
			var f = k<0?`Level ${-k}`:`Level ${k-0+1}`;
			var i = $("<img>").attr("id", "layer"+k).attr("height", "50px");
			generateTR([f, i]).appendTo($table2);
		});	
	}
	
	var $div4 = $("<div>").appendTo($("#right")).append("<strong>2D Map Data</strong>");
	function exportButton(text, format) {
		$('<button>', {
			'class' : 'exbtn',
			'text' : text,
			'disabled' : true,
			'css' : {
				'font-size' : '200%'
			},
			'on' : {
				'click' : function(event) {
					var refid = $('input[name=anchor]:checked').val();
					if (!refid) {
						alert('Need to select an anchor of floor plan for export');
						$mmfile.val('');
						return;
					}
					if ($mmfile.is(':visible')) {
						if (!$mmfile[0].files[0]) {
							alert('Need to specify a geoJSON file');
							return;
						}
					}
					composeData(format, (event.ctrlKey || event.metaKey) ? '&uuid=true' : '');
				}
			}
		}).appendTo($div4);
	}
	exportButton('Export', 'json');
	exportButton('ZIP', 'zip');
}
function composeData(format, opt) {
	var refid = $("input[name=anchor]:checked").val();
	var refpoint = refpointsMap[refid];
	var beacons_json = convertBeacons(refpoints, refpoint);
	var beacons = csvBeacons(beacons_json);
	var temp = [];
	Object.keys(layers).sort().forEach(function(key) {
		temp.push(layers[key]);
	});
	var json = {
			anchor: getAnchor(refpoint, true),
			samples: [{
				data:null
			}],
			beacons: [{
				data:beacons
			}],
			layers: temp
	};
	if (format == 'zip') {
		json.beacons.push({
			data:beacons_json
		});
	}
	$('form#compose_form').remove();
	function submit_compose_form() {
		var form = $('<form>', {
			'id' : 'compose_form',
			'method' : 'post',
			'action' : getDownloadURL('Compose', '2dMapData.' + format) + opt
		}).append($('<input>', {
			'type' : 'hidden',
			'name' : 'model',
			'value' : JSON.stringify(json)
		})).appendTo($('body')).submit();
	}
	$.ajax('https://www.ngdc.noaa.gov/geomag-web/calculators/calculateDeclination', {
		'type' : 'get',
		'dataType' : 'xml',
		'data' : {
			'lat1' : json.anchor.latitude,
			'lon1' : json.anchor.longitude,
			'key' : '<Enter your access key here>',
			'resultFormat' : 'xml'
		},
		'success' : function(xml) {
			try {
				var result = $(xml).find('result');
				[ 'declination' ].forEach(function(name) {
					var val = Number(result.find(name).text());
					isNaN(val) || (json.anchor[name] = val);
				});
			} catch(e) {
				console.error(e);
				if (!confirm('Error on calculateDeclination.\nContinue anyway?')) {
					return;
				}
			}
			submit_compose_form();
		},
		'error' : function(XMLHttpRequest, textStatus, errorThrown) {
			console.error(textStatus + ' (' + XMLHttpRequest.status + '): ' + errorThrown);
			if (!confirm('Error on calculateDeclination.\nContinue anyway?')) {
				return;
			}
			submit_compose_form();
		}
	});
}
var canvas1 = $("<canvas>")[0];
var canvas2 = $("<canvas>")[0];
function generateMMImage(layer, geojson, refpoint, samplings) {
	var hokoukukan = $hokoukukan(geojson);
	var anchor = getAnchor(refpoint);
	var extent = hokoukukan.calculateExtent(anchor);
	var ppm = 10;
	
	var margin = 15;
	var minx = extent[0] - margin, miny = extent[1] - margin;
	var maxx = extent[2] + margin, maxy = extent[3] + margin;
	var width = Math.abs(maxx-minx) * ppm;
	var height = Math.abs(maxy-miny) * ppm;
	var originx = -minx * ppm;
	var originy = height + miny * ppm;

	var drawImages = $("#ch_drawimages")[0].checked;
	var drawLinks = $("#ch_drawlinks")[0].checked;
	var drawSamples = $("#ch_drawsamples")[0].checked;
	
	// for debug, draw floor plan on the second image
	canvas2.width = width;
	canvas2.height = height;
	var ctx2 = canvas2.getContext("2d");
	ctx2.fillStyle = "white";
	ctx2.fillRect(0,0,width,height);	
	ctx2.save();
	ctx2.translate(originx, originy);
	ctx2.scale(ppm, -ppm);
	// draw black bold line
	hokoukukan.drawBack(ctx2, layer, anchor);
	ctx2.restore();
	
	var refids = $("#refpoints_table input[type=checkbox]:checked").map(function(){return $(this).val()}).toArray();
	
	if (drawSamples) {
		ctx2.save();
		ctx2.translate(originx, originy);
		ctx2.scale(ppm, -ppm);
		
		samplings.filter(function(sp){
			return refids.indexOf(sp.information.refid.$oid) > -1;
		}).map(function(s) {
			var info = s.information;
			if (info.floor_num == layer.floor_num) {
				var local = info;			
				if (!refpointsMap[info.refid.$oid]) {return}
				
				var anchor2 = getAnchor(refpointsMap[info.refid.$oid]); 
				var global = xy2latlon(local, anchor2)
				var local2 = latlon2xy(global, anchor);
				ctx2.globalAlpha = 1.0;
				ctx2.fillStyle = "white";
				ctx2.beginPath();
				ctx2.moveTo(local2.x, local2.y);
				ctx2.arc(local2.x, local2.y, 1.5, 0, Math.PI*2);
				ctx2.fill();
			}		
		});
		ctx2.restore();
	}
	
	
	if (drawImages) {	
		ctx2.save();
		ctx2.scale(ppm, ppm);
		
		refids.map(function(refid) {
			var fp = refpointFloorplanMap[refid];
			if (!fp) {
				return;
			}
			var f = parseInt(fp.floor);
			if (f != layer.floor_num) {
				return;
			}
			if (!fp.image) {
				return;
			}

			//console.log(fp);
			ctx2.save();
			var global = {lat: fp.lat, lon: fp.lng};
			var local = latlon2xy(global, anchor);
			//console.log(local);
			//console.log([-fp.origin_x/fp.ppm_x, -(fp.height-fp.origin_y)/fp.ppm_y]);
			ctx2.translate(local.x, -local.y);
			ctx2.translate(originx/ppm, originy/ppm);
			ctx2.rotate((fp.rotate - refpoint.anchor_rotate) / 180 * Math.PI);
			ctx2.translate(-fp.origin_x/fp.ppm_x, -(fp.height-fp.origin_y)/fp.ppm_y);
			//ctx2.globalAlpha = 0.5;
			ctx2.drawImage(fp.image, 0, 0, fp.width, fp.height, 0, 0, fp.width/fp.ppm_x, fp.height/fp.ppm_y);
			ctx2.restore();
		});
		ctx2.restore();
	}	
	
	
	// draw map matching image based on link network
	// draw 15 meters black then draw 4 meters white (changed by 有効幅員)
	
	if (drawLinks) {
		canvas1.width = width;
		canvas1.height = height;
		var ctx = canvas1.getContext("2d");
		ctx.save();
		ctx.clearRect(0,0,width,height);	
		ctx.translate(originx, originy);
		ctx.scale(ppm, -ppm);
		// draw links
		hokoukukan.drawLinks(ctx, layer, anchor);
		ctx2.drawImage(canvas1, 0, 0);
	}
	
	layer.data = canvas2.toDataURL();
	
	layer.param = {
		ppmx: ppm,
		ppmy: -ppm,
		ppmz: 1,
		originx: originx,
		originy: originy, 
		originz: 0, 
		floor: layer.floor_num
	};
}
function generateTR(list) {
	var $tr = $("<tr>");
	list.forEach(function(item) {
		$("<td>").append(item).appendTo($tr);		
	});
	return $tr;
}
function downloadFile(data, type, filename) {  
	var mime = "text/plain";
	if (type == "json") mime = 'text/json;charset=utf-8;';
	if (type == "csv") mime = 'text/comma-separated-values';
	
	  var blob = new Blob([data], { type: mime });
	  if (navigator.msSaveBlob) {
	    navigator.msSaveBlob(blob, filename);
	  } else {
	    var link = document.createElement("a");
	    if (link.download !== undefined) {
	      var url = URL.createObjectURL(blob);
	      link.setAttribute("href", url);
	      link.setAttribute("download", filename);
	    } else {        
	      link.href = `data:attachment/${type},` + data;
	    }
	    link.style = "visibility:hidden";
	    document.body.appendChild(link);
	    link.click();
	    document.body.removeChild(link);
	  }
	}
function countSamplings(match, callback) {
	dataUtil.getData({
		'type' : 'samplings',
		'data' : {
			'pipeline' : JSON.stringify([ {'$match' : match}, {
				'$group' : {
					'_id' : '$information.refid',
					'count' : {
						'$sum' : 1
					}
				}
			}])
		},
		'success' : function(data) {
			var counts = {};
			data.forEach(function(sc) {
				counts[sc._id.$oid] = sc.count;
			});
			callback(counts);
		}
	});
}

function infoSamplings(load, callback) {
	if (load) {
		dataUtil.getData({
			'type' : 'samplings',
			'data' : {
				'pipeline' : JSON.stringify({
					'$project' : {
						'_id' : 0,
						'information.x' : '$information.x',
						'information.y' : '$information.y',
						'information.floor_num' : '$information.floor_num',
						'information.refid' : '$information.refid'
					}
				})
			},
			'success' : callback
		});
	} else {
		callback();
	}
}

function exportSamplings(format, filename, opt) {
	var refpoint = $('input[name=anchor]:checked').val();
	if (refpoint) {
		location.href = getDownloadURL(format, filename) + (opt || '');
	} else {
		alert('Need to select an anchor of floor plan for export');
	}
}

function getDownloadURL(format, filename) {
	var refpoint = $('input[name=anchor]:checked').val();
	var url = 'data?' + new Date().getTime();
	url += '&' + $.param({
		'export' : format,
		'refpoint' : refpoint
	});
	$('#refpoints_table input[type=checkbox]:checked').each(function() {
		url += '&' + $.param({
			'refid' : $(this).val()
		});
	});
	if (filename) {
		url += '&' + $.param({
			'filename' : filename
		});
	}
	return url;
}
