window.currentSkin = localStorage.getItem('selectedSkin') || 'default';

function changeSkin(skinName) {
    document.cookie = 'selectedSkin=' + skinName + '; path=/; max-age=31536000; SameSite=Lax';
    localStorage.setItem('selectedSkin', skinName);
    location.reload();
}