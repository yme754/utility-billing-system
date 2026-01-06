import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ConsumerService } from '../../core/services/consumer';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  activeTab: string = 'ELECTRICITY';
  tariffs: any[] = [];
  filteredTariffs: any[] = [];
  showScrollBtn = false;    
  totalConsumers: number = 0; 
  billsProcessed: number = 0;

  constructor(private consumerService: ConsumerService) {}

  ngOnInit(): void {
    this.loadTariffs();
    this.loadStats();
  }

  loadStats() {
    this.consumerService.getConsumerCount().subscribe({
      next: (count) => this.totalConsumers = count,
      error: (err) => console.log('Stats error (Consumers)', err)
    });
    this.consumerService.getTotalBillsProcessed().subscribe({
      next: (count) => this.billsProcessed = count,
      error: (err) => console.log('Stats error (Bills)', err)
    });
  }

  loadTariffs() {
    this.consumerService.getPublicTariffPlans().subscribe({
      next: (data) => {
        console.log('Tariffs Loaded:', data);
        this.tariffs = data || [];
        this.filterTariffs('ELECTRICITY');
      },
      error: (err) => {
        console.error('Failed to load tariffs:', err);
        this.tariffs = [];
        this.filteredTariffs = [];
      }
    });
  }

  filterTariffs(type: string) {
    this.activeTab = type;
    this.filteredTariffs = this.tariffs.filter(t => t.utilityType === type);
  }

  scrollToSection(sectionId: string) {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.showScrollBtn = window.pageYOffset > 300;
  }

  scrollToTop() {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}