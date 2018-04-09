
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
