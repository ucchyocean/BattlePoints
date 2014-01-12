var psize = 3;

function load(_type, _page, _size, _filter) {
	type = _type;
	page = _page;
	size = _size;
	filter = _filter;
	$.ajax({
		url: "sort_data?type=" + type + "&page=" + page + "&size=" + size + "&filter=" + filter,
		dataType: 'xml',
		success : function(data){
			$("#stat-data").html("");
			total = $("battlepoints",data).attr("size");
			$("data",data).each(function(){
				var name = $(this).attr("name");
				$("#stat-data").append("<tr>"
						+ "<td class=\"name\"><img src=\"/faces/" + name  + ".png\" alt=\"" + name + "\" width=16 height=16>&nbsp;" + name + "</td>"
						+ "<td>" + $(this).find("cls").text() + "</td>"
						+ "<td>" + $(this).find("point").text() + "<br/>"
						+ "<span style=\"color: silver\">" + $(this).find("rank").text() + "</span></td>"
						+ "<td>" + $(this).find("rate").text() + "<br/>"
						+ "<span style=\"color: silver\">" + $(this).find("rrank").text() + "</span></td>"
						+ "<td>" + $(this).find("kill").text() + "<br/>"
						+ "<span style=\"color: silver\">" + $(this).find("krank").text() + "</span></td>"
						+ "<td>" + $(this).find("death").text() + "<br/>"
						+ "<span style=\"color: silver\">" + $(this).find("drank").text() + "</span></td>"
						+ "</tr>");
			});
			refreshPagination();
		}
	})
};

function refreshPagination() {
	$("#pagination").html("");
	if ( total == -1 ) {
		return;
	}
	var maxpage = Math.ceil(total/size);
	
	if ( page <= (psize + 1) ) {
		$("#pagination").append(
				"<button type=\"button\" class=\"btn btn-default disabled\">-</button>");
	} else {
		$("#pagination").append(
				"<button type=\"button\" class=\"btn btn-default\" onClick=\"changePage(1)\">1</button>");
	}
	
	for ( var i=-psize; i<=psize; i++ ) {
		if ( i == 0 ) {
			$("#pagination").append(
					"<button type=\"button\" class=\"btn btn-primary\">" + page + "</button>");
		} else if ( (page + i) <= 0 || maxpage < (page + i) ) {
			$("#pagination").append(
					"<button type=\"button\" class=\"btn btn-default disabled\">-</button>");
		} else {
			$("#pagination").append(
					"<button type=\"button\" class=\"btn btn-default\" onClick=\"changePage(" + (page+i) + ")\">" + (page+i) + "</button>");
		}
	}
	
	if ( page >= (maxpage-psize) ) {
		$("#pagination").append(
				"<button type=\"button\" class=\"btn btn-default disabled\"><a href=\"#\">-</a></button>");
	} else {
		$("#pagination").append(
				"<button type=\"button\" class=\"btn btn-default\" onClick=\"changePage(" + maxpage + ")\">" + maxpage + "</button>");
	}
}

function changePage(_page) {
	page = _page;
	load(type, page, size, filter);
}

function changePageSize(_size) {
	page = 1;
	size = _size;
	load(type, page, size, filter);
}

function changeType(_type) {
	$("#" + type + "_menu").removeClass("active");
	$("#" + type + "_head").removeClass("active");
	page = 1;
	type = _type;
	$("#" + type + "_menu").addClass("active");
	$("#" + type + "_head").addClass("active");
	load(type, page, size, filter);
}

function changeFilter(_filter) {
	filter = _filter;
	page = 1;
	load(type, page, size, filter);
}


var type = "point";
var page = 1;
var size = 10;
var filter = "";
var total = -1;

var req = location.search.replace(/^\?(.*)$/, '$1');
var getarr = req.split("&");
for ( var i=0; i<getarr.length; i++ ) {
	var str = getarr[i];
	if ( str.lastIndexOf("type=", 0) == 0 ) {
		type = str.slice(5);
	} else if ( str.lastIndexOf("page=", 0) == 0 ) {
		if ( !isNaN(str.slice(5)) ) {
			page = Number(str.slice(5));
		}
	} else if ( str.lastIndexOf("size=", 0) == 0 ) {
		if ( !isNaN(str.slice(5)) ) {
			size = Number(str.slice(5));
		}
	} else if ( str.lastIndexOf("filter=", 0) == 0 ) {
		filter = str.slice(7);
	}
}
load(type, page, 10, filter);
$(document).ready(function() {
	$("#" + type + "_menu").addClass("active");
	$("#" + type + "_head").addClass("active");
	if ( filter.length > 0 ) {
		$("#pfilter").val(filter);
	}
});

