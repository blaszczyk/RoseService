var entityNames = [];
var entityModels = {};

var $content;
var $header;

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
	appendEntityTable($content,name);
	$content.slideDown(500);
};

function showEntity(name,id) {
	$content.html('<h1>'+name+' '+id+'</h1><ul/>');
	$ul=$content.find('ul');
	getEntities(name,id,entity => {
		$.each(entityModels[name].fields, (i,f) => {
			$ul.append('<li>'+f.name+': '+displayValue(entity,f)+'</li>');
		});
		$content.slideDown(500);
	});
};

function appendEntityTable($parent,entityName,query) {
	var model = entityModels[entityName];
	
	$div=$parent.append($('#template-table').html()).find('div').last();
	$table = $div.find('table');
	$tablebody = $table.find('tbody');
	$pagesize = $div.find('.pagesize');
	$page = $div.find('.page');

	var count = 0;
	
	function pageSize() {
		var pageSize = $pagesize.val();
		return (pageSize === 'All') ? count : pageSize;
	};
	
	function maxPage() {
		return Math.floor( (count - 1) / pageSize() + 1 );
	};
	
	function navigate(clazz,event,transform) {
		$div.find('.'+clazz).on(event, e=> {
			var page = parseInt($page.val());
			if(transform)
				page = transform(page);
			if(page) {
				page = Math.max(1,Math.min(page,maxPage()));
				$page.val(page);
				showEntities();
			}
		});
	};

	navigate('page','change');
	navigate('pagesize','change');
	navigate('first','click',v => 1);
	navigate('prev','click',v => v-1);
	navigate('next','click',v => v+1);
	navigate('last','click',v => maxPage());

	$tableheader = $table.find('.header');
	$tableheader.append('<th>id</th>');
	$.each(model.fields,(i,f) => {
		$tableheader.append('<th>'+f.name+'</th>');
	});
	
	function showEntities() {
		$tablebody.find('tr.entity').remove();
		var page = $page.val();
		var pagesize = pageSize();
		var first = (page-1)*pagesize;
		var last = page*pagesize;
		var paginatedQuery = Object.assign({firstResult:first,maxResults:pagesize},query);
		getEntities(entityName, paginatedQuery, entities => {
			$.each(entities, (i,entity) => {
				$row=$tablebody.append('<tr class=entity data-entity='+entityName+' data-id='+entity.id+' />').find('tr').last();
				$row.append('<td>'+entity.id+'</td>');
				$.each(model.fields,(j,f) => {
					$row.append('<td>'+displayValue(entity,f)+'</td>');
				});
			});
		});
	};

	getEntityCount(entityName,query,c => {
		count = c;
		showEntities();
	});
};

function displayValue(entity,field) {
	var value;
	if(field.fieldType.endsWith('TOONE'))
		value = entityToString(entity[field.name],field.entity);
	else if(field.fieldType.endsWith('TOMANY'))
		value=entity[field.name+'_count'];
	else
		var value=entity[field.name];
	return value;
};

function entityToString(entity,entityName) {
	if(!entity)
		return "";
	var model = entityModels[entityName];
	var result = model.toString;
	$.each(model.fields, (i,f) => {
		if(result.indexOf('%'+f.name) >= 0)
			result = result.replace('%'+f.name, displayValue(entity,f));
	});
	return result;
}

$(function(){
	
	$content = $('#content');
	$header = $('#header');
	
	$header.delegate('.header','click', e => {
		var $this = $(e.target).closest('.header');
		var type = $this.attr('data-type');
		if(type)
			showContent(type);
	});

	$content.delegate('.entity','click', e => {
		var $this = $(e.target).closest('.entity');
		var entityName = $this.attr('data-entity');
		var id = $this.attr('data-id');
		if(entityName)
			showContent(entityName,id);
	});
	
	$content.delegate('tr.entity','mouseover', e => {
		$tr = $(e.target).closest('tr');
		$tr.css('background','#cccccc');
	});
	
	$content.delegate('tr.entity','mouseout', e => {
		$tr = $(e.target).closest('tr');
		$tr.css('background','#ffffff');
	});
	
	getModels( models => {
			$.each(models,(i,m) => {
				entityNames[i]=m.name;
				entityModels[m.name]=m;
			});
			showStart();
	});
});
