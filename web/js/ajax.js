
function getEntities(name,callback,query) {
	var url = '../entity/' + name;
	var data = {};
	if(query) {
		if(Array.isArray(query))
			data.id=query.toString();
		else if(Number.isInteger(query))
			url+='/'+query;
		else
			data = query;
	}
	$.ajax({
		type:'GET',
		url:url,
		data:data,
		success: e => {
			var entities = JSON.parse(e);
			callback(entities);
		},
		error:console.error
	});
};

function getEntityIds(name,callback) {
	var url = '../entity/' + name + '/id';
	$.ajax({
		type:'GET',
		url:url,
		success: e => {
			var entities = JSON.parse(e);
			callback(entities);
		},
		error:console.error
	});
};

function getEntityCount(name,callback,query) {
	var url = '../entity/' + name + '/count';
	$.ajax({
		type:'GET',
		url:url,
		data:query,
		success: callback,
		error:console.error
	});
};

function postEntity(name,entity,callback) {
	var url = '../entity/' + name;
	$.ajax({
		type:'POST',
		url:url,
		data:entity,
		success: e => {
			var entity = JSON.parse(e);
			callback(entity);
		},
		error:console.error
	});
};

function putEntity(name,entity,callback) {
	var url = '../entity/' + name + '/' + entity.id;
	$.ajax({
		type:'PUT',
		url:url,
		data:entity,
		success: callback,
		error:console.error
	});
};

function deleteEntity(name,id,callback) {
	var url = '../entity/' + name + '/' + id;
	$.ajax({
		type:'DELETE',
		url:url,
		data:entity,
		success: callback,
		error:console.error
	});
};

function getModels(callback) {
	$.ajax({
		type:'GET',
		url:'../server/models',
		success: e => {
			var models = JSON.parse(e);
			callback(models);
		},
		error: e => {
			console.error('Error recieving entity models',e);
		}
	});
};
