var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var game = require('game').game;
var status = require('game').status;
var renderStatus = require('../view/status');

function renderTr(move, ply, curPly) {
  return move ? {
    tag: 'div',
    attrs: {
      class: 'move' + (ply === curPly ? ' active' : '') + (ply%2===1 ? ' top': ''),
      'data-ply': ply
    },
    children: [move]
  } : {
    tag: 'div',
    attrs: {
      class: 'move bottom',
      'data-ply': ply
    },
    children: m.trust('&nbsp;')
  };
}

function renderTable(ctrl, curPly) {
  var moves = ctrl.root.data.game.moves;
  var pairs = [];
  for (var i = 0; i < moves.length; i += 2) pairs.push([moves[i], moves[i + 1]]);
  var result;
  if (status.finished(ctrl.root.data)) switch (ctrl.root.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  var tds = pairs.map(function(pair, i) {
    return m('td', [
      renderTr(pair[0], 2 * i + 1, curPly),
      renderTr(pair[1], 2 * i + 2, curPly),
      m('div.index', [i + 1])
    ]);
  });
  if (result) {
    var winner = game.getPlayer(ctrl.root.data, ctrl.root.data.game.winner);
    tds.push(m('td', [
      m('div.status'+(moves.length%2==1?'.black':''), [
        renderStatus(ctrl.root), m.trust('<br>'),
        winner ? ctrl.root.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ]),
      m('div.index.score'+(moves.length%2==1?'.black':''), [result])
    ]));
  }
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
          if (ply) ctrl.jump(parseInt(ply));
        }
      },
      tds));
}

function renderButtons(ctrl, curPly) {
  var nbMoves = ctrl.root.data.game.moves.length;
  var root = ctrl.root;
  var flipAttrs = {
    class: 'button flip hint--top' + (root.vm.flip ? ' active' : ''),
    'data-hint': root.trans('flipBoard'),
  };
  if (root.data.tv) flipAttrs.href = '/tv' + (root.data.tv.flip ? '' : '?flip=1');
  else if (root.data.player.spectator) flipAttrs.href = root.router.Round.watcher(root.data.game.id, root.data.opponent.color).url;
  else flipAttrs.onclick = root.flip;
  return m('div.buttons', [
    m('a', flipAttrs, m('span[data-icon=B]')), m('div.hint--top', {
      'data-hint': 'Tip: use your keyboard arrow keys!'
    }, [
      ['first', 'W', 1],
      ['prev', 'Y', curPly - 1],
      ['next', 'X', curPly + 1],
      ['last', 'V', nbMoves]
    ].map(function(b) {
      var enabled = curPly != b[2] && b[2] >= 1 && b[2] <= nbMoves;
      return m('a', {
        class: 'button ' + b[0] + ' ' + classSet({
          disabled: (ctrl.broken || !enabled),
          glowing: ctrl.vm.late && b[0] === 'last'
        }),
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    }))
  ]);
}

function autoScroll(movelist, ctrl) {
  var plyEl = movelist.querySelector('.active');
  if (plyEl) {
    ply = parseInt(plyEl.getAttribute('data-ply'));
    if (ply === ctrl.root.data.game.moves.length) {
      movelist.scrollLeft = ply*28 + 180;
    } else {
      movelist.scrollLeft = ply*28 - 140
    }
  }
  
}

module.exports = function(ctrl) {
  var curPly = ctrl.active ? ctrl.ply : ctrl.root.data.game.moves.length;
  var h = curPly + ctrl.root.data.game.moves.join('') + ctrl.root.vm.flip;
  if (ctrl.vm.hash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.hash = h;
  return m('div.replay', [
    renderButtons(ctrl, curPly),
    ctrl.enabledByPref() ? m('div.moves', {
      config: function(el, isUpdate) {
        autoScroll(el, ctrl);
        if (!isUpdate) setTimeout(partial(autoScroll, el, ctrl), 100);
      },
      onmousewheel: function(e) {
        if (Math.abs(e.wheelDeltaY) > Math.abs(e.wheelDeltaX)) {
          this.scrollLeft -= 0.2*e.wheelDeltaY;
        } else {
          this.scrollLeft -= 0.2*e.wheelDeltaX;
        }
        event.preventDefault();
      }
    }, renderTable(ctrl, curPly)) : null
  ]);
}
