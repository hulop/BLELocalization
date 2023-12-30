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

$(document).ready(function() {
	var table = window.t = $('#table').DataTable({
		'stateSave': true,
		'columnDefs' : [ {
			'name' : 'db_name',
			'targets' : 0
		}, {
			'name' : 'group_name',
			'targets' : 1
		} ]
	});

	$('#adddb').click(function() {
		$('#dbname').val() && $.ajax({
			'url' : 'dbs?name=' + $('#dbname').val() + '&group=' + $('#group_name').val(),
			'method' : 'POST',
			'success' : function(data) {
				window.location.reload();
			}
		});
	});

	$('#edit_group').click(function() {
		table.cells(null, 'group_name:name').nodes().to$().prop('contenteditable', true);
		$('#edit_group').hide();
		$('#cancel_group, #save_group').show();
	});

	$('#cancel_group').click(function() {
		table.cells(null, 'group_name:name').nodes().to$().prop('contenteditable', false);
		$('#edit_group').show();
		$('#cancel_group, #save_group').hide();
		table.cells(null, 'group_name:name').every(function() {
			this.data(this.data());
		});
	}).hide();

	$('#save_group').click(function() {
		var updates = [];
		table.rows().every(function() {
			var tr = this.nodes().to$();
			var group_name = tr.find('.group_name').text();
			if (group_name != this.data()[1]) {
				updates.push({
					'_id' : tr.find('.db_name').text(),
					'group' : group_name
				});
			}
		});
		if (updates.length > 0) {
			console.log(updates)
			$.ajax({
				'type' : 'POST',
				'url' : location.href,
				'processData' : false,
				'contentType' : 'application/json',
				'data' : JSON.stringify(updates),
				'success' : function(data) {
					location.reload();
				},
				'error' : function(XMLHttpRequest, textStatus, errorThrown) {
					console.error(textStatus + ' (' + XMLHttpRequest.status + '): ' + errorThrown);
					errorThrown && alert(textStatus + ' (' + XMLHttpRequest.status + '): ' + errorThrown);
				}
			});
		}
	}).hide();
});
