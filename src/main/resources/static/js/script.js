//HOME PAGE START
// script.js - behavior centralized
document.addEventListener('DOMContentLoaded', function(){
// Smooth scrolling for navigation links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const href = this.getAttribute('href');
            if (!href || href === '#') return; // ignore empty anchors
            const target = document.querySelector(href);
            if (!target) return;
            e.preventDefault();
            const offsetTop = target.offsetTop - 70;
            window.scrollTo({ top: offsetTop, behavior: 'smooth' });
        });
    });


// Add active class to navigation shadow on scroll
    const navbar = document.querySelector('.navbar');
    window.addEventListener('scroll', function() {
        if (!navbar) return;
        if (window.scrollY > 50) navbar.classList.add('shadow-lg');
        else navbar.classList.remove('shadow-lg');
    });


// Floating button subtle animation
    const floatingButton = document.querySelector('.position-fixed .btn-warning');
    if (floatingButton){
        setInterval(() => {
            floatingButton.style.transform = 'scale(1.08)';
            setTimeout(() => { floatingButton.style.transform = 'scale(1)'; }, 300);
        }, 3000);
    }


// Quick action cards click handlers
    document.querySelectorAll('.bg-light .card').forEach(card => {
        card.addEventListener('click', function() {
            const icon = this.querySelector('i');
            if (!icon) return;
            if (icon.classList.contains('fa-question-circle')) {
                const el = document.getElementById('askQuestionModal');
                if (el) new bootstrap.Modal(el).show();
            } else if (icon.classList.contains('fa-comments')) {
                const el = document.getElementById('chatModal');
                if (el) new bootstrap.Modal(el).show();
            }
        });
    });


// Auto-suggest consultants based on question category (placeholder)
    const category = document.getElementById('questionCategory');
    if (category){
        category.addEventListener('change', function(){
            console.log('Category changed to:', this.value);
// TODO: fetch suggestions via AJAX / websocket
        });
    }
});
// Load animation hero homepage
document.addEventListener("DOMContentLoaded", function () {
    lottie.loadAnimation({
        container: document.getElementById("chat-animation"),
        renderer: "svg",
        loop: true,
        autoplay: true,
        path: "https://assets9.lottiefiles.com/packages/lf20_V9t630.json"
    });
});
// HOME PAGE END