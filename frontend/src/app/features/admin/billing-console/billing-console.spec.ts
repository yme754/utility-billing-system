import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BillingConsoleComponent } from './billing-console';

describe('BillingConsole', () => {
  let component: BillingConsoleComponent;
  let fixture: ComponentFixture<BillingConsoleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingConsoleComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BillingConsoleComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
