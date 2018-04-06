var entityNames = [];
var entityModels = {};

var $content;
var $header;

function getEntities(name,callback,id) {
	var url = '../entity/' + name;
	var data = {};
	if(id) {
		if(Array.isArray(id))
			data.id=id.toString();
		else
			url+='/'+id;
	}
	$.ajax({
		type:'GET',
		url:url,
		data:data,
		success: e => {
			var entity = JSON.parse(e);
			callback(entity);
		},
		error:console.error
	});
};

function showContent(contentType,id) {
	$content.hide();
	var shower = window['show'+contentType];
	if(shower)
		shower();
	else if(entityModels[contentType])
		if(id)
			showEntity(contentType,id);
		else
			showEntityList(contentType);
}

function showStart() {
	$content.html('<h1>Start</h1><br/>');
	$.each(entityNames, (i,e) => {
		$content.append('<button class=entity data-entity='+e+'>'+e+'</button><br/>');
	});
	$content.slideDown(500);
};

function showServer() {
	console.log('server');
};

function showFiles() {
	console.log('files');
};

function showEntityList(name) {
	$content.html('<h1>'+name+'</h1>');
	getEntities(name,entities => {
		appendEntityTable($content,name,entities);
		$content.find('h1').append(' #'+entities.length);
		$content.slideDown(2500);
	});
};

function showEntity(name,id) {
	$content.html('<h1>'+name+' '+id+'</h1><ul/>');
	$ul=$content.find('ul');
	getEntities(name,entity => {
		$.each(entityModels[name].fields, (i,f) => {
			$ul.append('<li>'+f.name+': '+entity[f.name]+'</li>');
		});
		$content.slideDown(2500);
	},id);
};

function appendEntityTable($parent,entityName,entities) {
	var model = entityModels[entityName];
	$table = $parent.append('<table/>').last();
	$tableheader = $table.append('<tr/>').last();
	$tableheader.append('<th>id</th>');
	$.each(model.fields,(i,f) => {
		$tableheader.append('<th>'+f.name+'</th>');
	});
	$.each(entities, (i,entity) => {
		$row=$table.append('<tr/>').last();
		$row.append('<td class=entity data-entity='+entityName+' data-id='+entity.id+' >'+entity.id+'</td>');
		$.each(model.fields,(j,f) => {
			$row.append('<td>'+displayValue(entity[f.name])+'</td>');
		});
	});
};

function displayValue(value) {
	return Array.isArray(value) ? '#'+value.length : value;
}

$(function(){
	
	$content = $('#content');
	$header = $('#header');

	$content.delegate('.entity','click', e => {
		var entityName = $(e.target).attr('data-entity');
		var id = $(e.target).attr('data-id');
		if(entityName)
			showContent(entityName,id);
	});
	
	$header.delegate('.header','click', e => {
		var type = $(e.target).attr('data-type');
		if(type)
			showContent(type);
	});
	
	// request models
	$.ajax({
		type:'GET',
		url:'../server/models',
		success: e => {
			var models = JSON.parse(e);
			$.each(models,(i,m) => {
				entityNames[i]=m.name;
				entityModels[m.name]=m;
			});
			showStart();
		},
		error: e => {
			alert('Error recieving entity models',e);
		}
	});
});
