// JS Debounce (http://davidwalsh.name/function-debounce)
function debounce(func, wait, immediate) {
	var timeout;
	return function() {
		var context = this, args = arguments;
		var later = function() {
			timeout = null;
			if (!immediate) func.apply(context, args);
		};
		var callNow = immediate && !timeout;
		clearTimeout(timeout);
		timeout = setTimeout(later, wait);
		if (callNow) func.apply(context, args);
	};
};


var pane = {
	step: 0,
	data: [],
	name: name,
	is_playing: false,
};


var data = [];
var vis = d3.select("#unterschied")
var margin = {
		top: 20,
		right: 20,
		bottom: 20,
		left: 50
}
var width = 800 - margin.left - margin.right
var height = 250 - margin.top - margin.bottom
var x = d3.time.scale().range([0, width]);
var y = d3.scale.linear().range([height, 0]);
var xAxis = d3.svg.axis()
	.scale(x)
	.orient("bottom");
var yAxis = d3.svg.axis()
	.scale(y)
	.orient("left");


var line = d3.svg.line()
	.x(function(d) { return x(d.step); })
	.y(function(d) { return y(d.diff); })

var svg = d3.select("#unterschied").append("svg")
	.attr("width", width + margin.left + margin.right)
	.attr("height", height + margin.top + margin.bottom)
	.append("g")
	.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

svg.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height + ")")
	.call(xAxis)
svg.append("g")
	.attr("class", "y axis")
	.call(yAxis)
	.append("text")
	.attr("transform", "rotate(-90)")
	.attr("y", 6)
	.attr("dy", ".71em")
	.style("text-anchor", "end")

svg.append("path")
	.attr("class", "line")
	.attr("d", line(data));

function update_chart() {
	// start = Math.max(0, pane.step - 20);
	// end = Math.min(diff_data.length-1, pane.step + 20)
	x.domain(d3.extent(data, function(d) { return d.step; }));
	y.domain(d3.extent(data, function(d) { return d.diff; }));
	var svg = d3.select("#unterschied").transition();

	svg.select(".line")
		.duration(500)
		.attr("d", line(data));
	svg.select(".x.axis")
		.duration(500)
		.call(xAxis);
	svg.select(".y.axis")
		.duration(500)
		.call(yAxis);
}
update_chart = debounce(update_chart, 500)

function draw() {
	update();
	var d = pane.data[pane.step];
}

$("#step_slider").slider({
	range: "max",
	min: 0,
	max: 0,
	value: 0,
	step: 1,
	slide: function (event, ui) {
		pane.step = ui.value;
		draw();
	}
});


function update() {
	var d = pane.data[pane.step];
	$.map(d.output, function(value, key) {
		$("#ai_"+key+"_output").val(value);
	})

	if (pane.is_playing) {
		$("#play_button").addClass("active");
		$("#pause_button").removeClass("active");
	} else {
		$("#play_button").removeClass("active");
		$("#pause_button").addClass("active");
	}

	//update_chart();
}

$(document).ready(function () {
	console.log("Streaming game data from", window.location.origin + $("#game_script").attr("stream"));
	var evtSrc = new EventSource(window.location.origin + $("#game_script").attr("stream"));
	NProgress.configure({ trickle: false });
	NProgress.start();

	evtSrc.onerror = function () {
		console.log("SSE Err");
		evtSrc.close();
		NProgress.done();
	};

	evtSrc.addEventListener("state", function(e) {
		NProgress.inc()
		console.log(e.data);
		d = JSON.parse(e.data);
		console.log(d);
		pane.data.push(d);
		//NProgress.set(d.progress);
		$("#step_slider").slider("option", "max", pane.data.length-1);
		var values = $.map(d.wonChips, function (value, key) {return value})
		var d = {}
		d.diff = values[0] - values[1];
		d.ai1_abs = 0;
		d.ai2_abs = 1;
		d.ai1_gain = values[0];
		d.ai2_gain = values[1];
		d.step = pane.data.length;
		data.push(d);
		update_chart();
		draw();
	});


	evtSrc.addEventListener("stream_stopped", function (e) {
		console.log(e);
		console.log("stream_stopped");
		evtSrc.close();
		NProgress.done();
	});

	// ## in ne generelle lib verschieben
	evtSrc.addEventListener("game_finished", function(e) {
		console.log("game_finished", e.data);
		$("#finished_message").show();
		$("#finished_message").find("a").attr("href", e.data);
	});

	// ## in ne generelle lib verschieben
	evtSrc.addEventListener("qualified", function(e) {
		$("#qualified_message").show();
		NProgress.done();
	});

	evtSrc.addEventListener("finished_transmitting", function(e) {
		console.log("finished_transmitting");
		NProgress.done();
	});
});
