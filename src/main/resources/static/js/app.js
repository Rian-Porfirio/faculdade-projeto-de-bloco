document.addEventListener('DOMContentLoaded', () => {

  // ---- Delete Modal Wiring ----------------------------------------
  const deleteModal = document.getElementById('deleteModal');
  if (deleteModal) {
    deleteModal.addEventListener('show.bs.modal', (event) => {
      const btn = event.relatedTarget;
      if (!btn) return;
      const productId   = btn.getAttribute('data-product-id');
      const productName = btn.getAttribute('data-product-name');

      const nameEl = document.getElementById('deleteProductName');
      const form   = document.getElementById('deleteForm');
      if (nameEl) nameEl.textContent = productName ?? 'este produto';
      if (form)   form.setAttribute('action', `/products/${productId}`);
    });
  }

  // ---- Description Char Counter ----------------------------------
  const descTextarea = document.getElementById('descriptionInput');
  const charCount    = document.getElementById('descCharCount');
  if (descTextarea && charCount) {
    const update = () => {
      charCount.textContent = `${descTextarea.value.length}/500`;
      charCount.classList.toggle('text-danger', descTextarea.value.length > 480);
    };
    descTextarea.addEventListener('input', update);
    update();
  }

  // ---- Auto-dismiss alerts after 5 s ------------------------------
  document.querySelectorAll('.alert.fade.show').forEach((el) => {
    setTimeout(() => {
      const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
      bsAlert?.close();
    }, 5000);
  });

  // ---- HTMX: preserve filter state on HTMX swap ------------------
  document.body.addEventListener('htmx:afterSwap', () => {
    // Rebind delete buttons that may have been swapped in via HTMX
    document.querySelectorAll('.btn-delete').forEach((btn) => {
      btn.setAttribute('data-bs-toggle', 'modal');
      btn.setAttribute('data-bs-target', '#deleteModal');
    });
  });

  // ---- Form: prevent double-submit --------------------------------
  const productForm = document.getElementById('productForm');
  if (productForm) {
    productForm.addEventListener('submit', function () {
      const submitBtn = this.querySelector('#btnSubmit');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Salvando...';
      }
    });
  }
});
