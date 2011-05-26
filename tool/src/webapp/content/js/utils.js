var sakai = sakai || {};
var utils = utils || {};
var evalsys = evalsys || {};

evalsys.instrumentBlockItem = function() {
	$('label.blockItemLabel,label.blockItemLabelNA').click(
			function() {
				var choiceGroup = $(this).parents('.choiceGroup');
				$(choiceGroup).find('label').removeClass(
						'blockItemLabelSelected').removeClass(
						'blockItemLabelSelectedNA');
				$(choiceGroup).parent().parent().find('.itemDoneCheck')
						.addClass('itemDoneCheckShow');

				if ($(this).hasClass('blockItemLabel')) {
					$(this).addClass('blockItemLabelSelected');
				} else {
					$(this).addClass('blockItemLabelSelectedNA');
				}
			});

	$('.blockItemLabel,.blockItemLabelNA').each(
			function() {
				if ($(this).children('input:checked').length == 1) {
					$(this).parents('.choiceGroup').parent().parent().find(
							'.itemDoneCheck').addClass('itemDoneCheckShow');
					if ($(this).hasClass('blockItemLabel')) {
						$(this).addClass('blockItemLabelSelected');
					} else {
						$(this).addClass('blockItemLabelSelectedNA');
					}
				}
			});
	$('.blockItemGroup').each(function(x) {
		var headerCol = [];
		$(this).find('.actualHeader').each(function() {
			headerCol.push($(this).text());
		});
		$(this).find('.actualHeaderNA').each(function() {
			headerCol.push($(this).text());
		});
		headerCol = headerCol.reverse();
		$(this).find('.choiceGroup').each(function() {
			$(this).find('span.blockChoice').each(function(n) {
				$(this).prepend(headerCol[n]);
				$(this).siblings('input').attr('title', headerCol[n]);
			});
		});
	});
};

evalsys.instrumentSteppedItem = function() {
	$('label.blockItemLabel,label.blockItemLabelNA').click(
			function() {
				var answerCell = $(this).parents('.answerCell');
				$(answerCell).find('label').removeClass(
						'blockItemLabelSelected').removeClass(
						'blockItemLabelSelectedNA');
				$(answerCell).find('.itemDoneCheck').addClass(
						'itemDoneCheckShow');
				if ($(this).hasClass('blockItemLabel')) {
					$(this).addClass('blockItemLabelSelected');
				} else {
					$(this).addClass('blockItemLabelSelectedNA');
				}
			});
	$('.blockItemLabel,.blockItemLabelNA').each(
			function() {
				if ($(this).children('input:checked').length == 1) {
					$(this).parents('.answerCell').find('.itemDoneCheck')
							.addClass('itemDoneCheckShow');
					if ($(this).hasClass('blockItemLabel')) {
						$(this).addClass('blockItemLabelSelected');
					} else {
						$(this).addClass('blockItemLabelSelectedNA');
					}
				}
			});
};

evalsys.instrumentMCMAItem = function() {
	$('.mult-choice-ans li.check').each(function() {
		if ($(this).find('input').attr('checked') === true) {
			$(this).addClass('checked');
		}
	});
	$('.mult-choice-ans li.na').each(function() {
		if ($(this).find('input').attr('checked') === true) {
			$(this).addClass('checkedNA');
		}
		if ($(this).find('input').length === 0) {
			$(this).hide();
		}
	});
	$('.mult-choice-ans input').click(
			function(e) {
				var className;
				var parentLi = $(this).parents('li:eq(0)');
				var parentUl = $(this).parents('ul:eq(0)');
				if ($(parentLi).hasClass('check')) {
					className = 'checked';
				} else {
					className = 'checkedNA';
				}
				if ($(this).attr('type') == 'radio') {
					$(parentUl).find('li').removeClass('checked').removeClass(
							'checkedNA');
					$(parentLi).addClass(className);
				} else {
					if ($(parentLi).hasClass('check')) {
						$(parentLi).toggleClass('checked');
					} else {
						$(parentLi).toggleClass('checkedNA');
					}
					if ($(parentLi).hasClass('checkedNA')) {
						$(this).parents('.mult-choice-ans')
								.children('li.check').find(
										'input[type!="hidden"]').attr(
										'checked', false);
						$(this).parents('.mult-choice-ans').find('li.check')
								.removeClass('checked');
					}
					if ($(parentLi).hasClass('check')
							| $(this).parents('li:eq(0)').hasClass('checked')) {
						$(this).parents('.mult-choice-ans').children('li.na')
								.find('input[type!="hidden"]').attr('checked',
										false);
						$(this).parents('.mult-choice-ans').find('li.na')
								.removeClass('checkedNA');
					} else {
					}
				}
				e.stopPropagation();
			});
};

evalsys.instrumentScaleItem = function() {
	$('.scaleItemLabel').click(
			function() {
				$(this).parents('.itemScalePanel').find('label').removeClass(
						'scaleItemLabelSelected');
				$(this).parents('li').find('.itemDoneCheck').addClass(
						'itemDoneCheckShow');
				$(this).addClass('scaleItemLabelSelected');
			});
	$('.scaleItemLabelNA').click(
			function() {
				$(this).parents('.itemScalePanel').find('label').removeClass(
						'scaleItemLabelSelected');
				$(this).parents('li').find('.itemDoneCheck').addClass(
						'itemDoneCheckShow');
				$(this).addClass('scaleItemLabelSelected');
			});
	$('.scaleItemLabel').each(
			function() {
				if ($(this).children('input:checked').length == 1) {
					$(this).parents('li').find('.itemDoneCheck').addClass(
							'itemDoneCheckShow');
					$(this).addClass('scaleItemLabelSelected');
				}
			});
};

evalsys.instrumentDisplayHorizontal = function() {
	$('.fullDisplayHorizontalScale').each(function() {
		$(this).find('input:checked').parent('span').addClass('labelSelected');
	});
	$('.fullDisplayHorizontalScale').find('input').click(function() {
		$(this).parents('table').find('span').removeClass('labelSelected');
		$(this).parent('span').addClass('labelSelected');
	});
};
