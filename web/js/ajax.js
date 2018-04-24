$.ajaxSettings.error=console.log;

function getEntities(name,query,callback) {
	var url = '../entity/' + name;
	var data = {};
	if(query) {
		if(Array.isArray(query))
			data.id=query.toString();
		else if(Number.isInteger(query) || typeof query === 'string')
			url+='/'+query;
		else
			data = query;
	}
	data.one='entity';
	data.many='count';
	$.ajax({
		type:'GET',
		url:url,
		data:data,
		success: e => {
			var entities = JSON.parse(e);
			if(Array.isArray(entities))
				$.each(entities,(i,entity) => {
					entity.entityName=name;
				});
			else
				entities.entityName=name;
			callback(entities);
		}
	});
};

function getEntityIds(name,callback) {
	var url = '../entity/' + name + '/id';
	$.ajax({
		type:'GET',
		url:url,
		success: e => {
			var ids = JSON.parse(e);
			callback(ids);
		}
	});
};

function getEntityCount(name,query,callback) {
	var url = '../entity/' + name + '/count';
	$.ajax({
		type:'GET',
		url:url,
		data:query,
		success: callback,
		error:console.error
	});
};

function postEntity(entity,callback) {
	var url = '../entity/' + entity.entityName;
	$.ajax({
		type:'POST',
		url:url,
		data:entity,
		success: e => {
			var entity = JSON.parse(e);
			e.entityName=entity.entityName;
			callback(e);
		}
	});
};

function putEntity(entity,callback) {
	var url = '../entity/' + entity.entityName + '/' + entity.id;
	$.ajax({
		type:'PUT',
		url:url,
		data:entity,
		success: callback
	});
};

function deleteEntity(entity,callback) {
	var url = '../entity/' + entity.entityName + '/' + entity.id;
	$.ajax({
		type:'DELETE',
		url:url,
		success: callback
	});
};

function getModels(callback) {
	$.ajax({
		type:'GET',
		url:'../server/models',
		success: e => {
			var models = JSON.parse(e);
			callback(models);
		}
	});
};
