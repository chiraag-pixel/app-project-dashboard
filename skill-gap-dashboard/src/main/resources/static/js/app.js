// Shared page behavior: upload loading indicator and destructive-action confirms.

document.addEventListener('submit', function (event) {
  var form = event.target;
  if (form.matches('form[data-loading]')) {
    var container = form.parentElement;
    var indicator = container ? container.querySelector('.loading-indicator') : null;
    if (indicator) {
      indicator.classList.add('show');
    }
    var button = form.querySelector('button[type="submit"]');
    if (button) {
      button.disabled = true;
      button.textContent = 'Processing...';
    }
  }
});

document.addEventListener('click', function (event) {
  var link = event.target.closest('[data-confirm]');
  if (link && !window.confirm(link.getAttribute('data-confirm'))) {
    event.preventDefault();
  }
});
